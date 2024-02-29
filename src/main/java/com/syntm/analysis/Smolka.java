package com.syntm.analysis;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.syntm.lts.CompressedTS;
import com.syntm.lts.State;
import com.syntm.lts.TS;
import com.syntm.lts.Trans;

import ch.qos.logback.core.util.SystemInfo;

/**
 * An object for reducing a transition system using the Kanellakis-Smolka
 * algorithm.
 * <p>
 * Estimated space complexity: O(c+m+n)
 * <p>
 * Estimated time complexity: O(cmn)
 * <p>
 * (c channels, m transitions, n states)
 */
public class Smolka implements java.io.Serializable {
    private Set<String> channels;
    // private Set<Set<State>> initPartition;
    private TS transSystem;
    // private Map<State, Integer> lastId;
    // private Set<State> parameterStates;
    private Map<State, Set<Set<State>>> epsilonMap;

    /**
     * Contructor for a class using the Kanellakis-Smolka algorithm to reduce the
     * transition system.
     * 
     * @param parameterStates All states in the parameter.
     * @param indexedFamily A concurrent hashmap with an initial partition mapped to
     *                      each state.
     * @param transSystem   The transition system to be reduced.
     * @param channels      A set of the available channels.
     * @return An object callable with <code>run()</code> to reduce the transition
     *         system using the Kannelakis-Smolka algorithm.
     */
    public Smolka(Map<State, Set<Set<State>>> epsilonMap, 
                  //Set<State> parameterStates, Set<Set<State>> initPartition,
            TS transSystem, Set<String> channels) {
        // this.initPartition = initPartition;
        this.epsilonMap = new HashMap<>(epsilonMap);
        // this.parameterStates = parameterStates;
        // this.lastId = new HashMap<>();
        // for (State s : parameterStates) {
        //     this.lastId.put(s, 0);
        //     for (Set<State> b : this.initPartition) {
        //         for (State t : b) { // Becomes O(n^2)
        //             t.setBlockId(s, this.lastId.get(s));
        //             // System.out.println(t.blockId);
        //         }
        //         this.lastId.compute(s, (k,x) -> x+1);
        //     }
        // }
        this.transSystem = transSystem;
        this.channels = channels; // Space complexity O(c)

    }

    /**
     * The method to call to run the Kanellakis-Smolka algorithm.
     * <p>
     * Time complexity: O(cmn)?
     * <p>
     * Space complexity: O(c+m+n)?
     * <p>
     * (c channels, m transitions, n states)
     * 
     * @return The reduced transition system.
     */
    public TS run() {
        Map<State, Queue<Set<State>>> piWaiting = new HashMap<>();
        Map<State, Set<Set<State>>> piBlocks = new HashMap<>(); 
        for (Entry<State, Set<Set<State>>> entry : this.epsilonMap.entrySet()) {
            piWaiting.put(entry.getKey(), new LinkedList<>(entry.getValue()));
            piBlocks.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        while (!piWaiting.values().stream().allMatch(s -> s.isEmpty())) {
            for (State epsilon : this.epsilonMap.keySet()) {
                Set<Set<State>> piWaitingTemp = new HashSet<>();
                Set<Set<State>> blocks = new HashSet<>();                
                while(!piWaiting.get(epsilon).isEmpty()){
                    Set<State> tempBlock;
                    tempBlock = piWaiting.get(epsilon).remove();
                    blocks.add(tempBlock);
                }
                
                for (String channel : this.channels) { 
                    for (Trans tr : epsilon.getTrans()) {
                        Set<Set<State>> ePartitions = new HashSet<>(this.epsilonMap.get(tr.getDestination()));
                        for (Set<State> ePrime : ePartitions) {
                            for (Set<State> block : blocks){
                                Set<State> splitter = applyEBisim(block, tr,ePrime, channel,epsilon);

                                if (!splitter.isEmpty() && !splitter.equals(block)) {                                    
                                    List<Set<State>> splitP; 
                                    splitP = split(block, splitter);
                                    piBlocks.get(epsilon).remove(block);
                                    piBlocks.get(epsilon).addAll(splitP);
                                    piWaitingTemp.addAll(splitP);
                                }
                            }     
                        }
                    }
                }
                if(!piWaiting.isEmpty()){
                    piWaiting.get(epsilon).addAll(piWaitingTemp);
                }
            }
            for (Entry<State, Set<Set<State>>> entry : piBlocks.entrySet()) {
                this.epsilonMap.put(entry.getKey(), entry.getValue());
            }
        }

        String initID = this.transSystem.getInitState().getId();
        Set<Set<State>> rhoFinal = new HashSet<>();

        for (Entry<State, Set<Set<State>>> entry : this.epsilonMap.entrySet()) {
            if (entry.getKey().getId().equals(initID)) {
                rhoFinal.addAll(entry.getValue());
                break;
            }
        }

        if (rhoFinal.size() == 1) return this.transSystem;

        CompressedTS c = new CompressedTS("s-" + this.transSystem.getName());
        return c.compressedTS(this.transSystem, rhoFinal);
    }

    /**
     * The split function used by the Kanellakis-Smolka algorithm.
     * <p>
     * Time complexity: O(m)?
     * <p>
     * Space complexity: O(m+n)?
     * <p>
     * (m transitions, n states)
     * 
     * @param st      The state with the partition
     * @param block   The splitter block
     * @param channel The channel/action to split on
     * @return The split block(s)
     */
    private Set<Set<State>> smolkaSplit(State st, Set<State> block, String channel) { // O(m)
        
        Set<State> block1 = new HashSet<>();
        Set<State> block2 = new HashSet<>();
        State s = block.iterator().next();

        // Find the blocks with transitions from s.
        Set<Integer> sBlocks = getDestinationBlocks(st, s, channel); // O(m)
        // int newId = getNewId(st);

        for (State t : block) { // Each transition is only checked once, so this becomes O(m)
            Set<Integer> blockDestinationPartitions = getDestinationBlocks(st, t, channel);
            // if s and t can reach the same set of blocks in π via a-labelled transitions
            if (blockDestinationPartitions.equals(sBlocks)) {
                block1.add(t);
            } else {
                // t.setBlockId(st, newId);
                block2.add(t);
            }
        }

        // if B2 is empty then return {B1} else return {B1, B2}
        Set<Set<State>> output = new HashSet<>();
        if (block2.isEmpty()) {
            output.add(block1);
        } else {
            output.add(block1);
            output.add(block2);
        }
        return output;
    }

    private List<Set<State>> split(Set<State> p, Set<State> splitter) {
        List<Set<State>> splitP = new LinkedList<>();
        Set<State> notsplitter = new HashSet<>(p);

        notsplitter.removeAll(splitter);
        //System.out.println(splitter.size() + "," + notsplitter.size());

        splitP.add(splitter);
        if (!notsplitter.isEmpty()) splitP.add(notsplitter);

        return splitP;
    }

    /**
     * A method for getting the blocks pointed to by transitions from a state
     * <code>t</code>.
     * 
     * @param st      The state with the partition
     * @param t       The origin state
     * @param channel The channel/action to follow
     * @param pi      The main partition
     * @return The set of blocks pointed to
     */
    private Set<Integer> getDestinationBlocks(State st, State t, String channel) { // O(m)
        Set<Integer> blockDestinationPartitions = new HashSet<>();
        Set<Trans> transitions = t.getTrans();
        for (Trans trans : transitions) { // O(m)
            if (trans.getAction().equals(channel)) {
                blockDestinationPartitions.add(trans.destination.getBlockId(st)); // O(1)
            }
        }
        return blockDestinationPartitions;
    }

    /**
     * Retrieves a new block id
     * 
     * @param s The state with the partition.
     * @return A new, previously unused, id
     */
    // private int getNewId(State s) {
    //     int id = this.lastId.get(s);
    //     this.lastId.put(s, id+1);
    //     return id+1;
    // }

     /**
     * Def 5.4 in the paper
     * @param p         The starting block
     * @param epsilon   A state (our addition)
     * @param trEpsilon A transition (from p to ePrime)
     * @param channel   A channel name
     * @return          A possible splitter for the block.
     */
    private Set<State> applyEBisim(Set<State> p, Trans trEpsilon, // O(mn^3)
                                   Set<State> ePrime, String channel, 
                                   State epsilon) {
        Set<State> out = new HashSet<>();
    
        if (epsilon.enable(epsilon, channel) // activating a channel
                && trEpsilon.getAction().equals(channel)) {
            for (State s : p) { // *O(n) but combines with the partition for loop outside the function
                for (State sPrime : p) { // *O(n)

                    if (s.canExactSilent(s.getOwner(), s, channel)
                            && !sPrime.canDirectReaction(sPrime.getOwner(), sPrime, channel)) {
                        if (sPrime.canExactSilent(sPrime.getOwner(), sPrime, channel)) {
                            if (ePrime.contains(s.takeExactSilent(s.getOwner(), s,
                                    channel).getDestination())
                                    && ePrime.contains(sPrime.takeExactSilent(sPrime.getOwner(), sPrime,
                                            channel).getDestination())) {
                                out.add(s);
                                out.add(sPrime);
                            }
                        } else {
                            if (!sPrime.getListen().getChannels().contains(channel)) {
                                if (ePrime.contains(s.takeExactSilent(s.getOwner(), s,
                                        channel).getDestination())
                                        && ePrime.contains(sPrime)) {
                                    out.add(s);
                                    out.add(sPrime);
                                }
                            }
                        }
                    }

                    if (!s.getListen().getChannels().contains(channel) &&
                            !sPrime.getListen().getChannels().contains(channel)) {
                        if (ePrime.contains(s) && ePrime.contains(sPrime)) {
                            out.add(s);
                            out.add(sPrime);
                        }
                    }

                    if (s.canDirectReaction(s.getOwner(), s, channel)
                            && !sPrime.canExactSilent(sPrime.getOwner(), sPrime, channel)) {
                        if (sPrime.canDirectReaction(sPrime.getOwner(), sPrime, channel)) {
                            if (ePrime.contains(s.takeDirectReaction(s.getOwner(), s, channel).getDestination())
                                    && ePrime
                                            .contains(sPrime.takeDirectReaction(sPrime.getOwner(), sPrime, channel)
                                                    .getDestination())) {
                                out.add(s);
                                out.add(sPrime);
                            }
                        } else {
                            Set<State> reach = sPrime.weakBFS(sPrime.getOwner(), sPrime, channel); // O(mn)
                            if (reach.size() != 1) { // O(mn)
                                for (State sReach : reach) { // O(n)
                                    if (sReach.canDirectReaction(sReach.getOwner(), sReach, channel)) { // O(m)
                                        if (ePrime
                                                .contains(sReach.takeDirectReaction(sReach.getOwner(), sReach,
                                                        channel).getDestination())
                                                &&
                                                ePrime.contains(s.takeDirectReaction(s.getOwner(), s,
                                                        channel).getDestination())) {
                                            out.add(s);
                                            out.add(sPrime);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    
        if (out.isEmpty() && !epsilon.canTakeInitiative(epsilon.getOwner(), epsilon, channel)
            && epsilon.getListen().getChannels().contains(channel)) {
            for (State s : p) {
                if (s.canTakeInitiative(s.getOwner(), s, channel)) {
                    if (ePrime.contains(s.takeInitiative(s.getOwner(), s, channel).getDestination())) {
                        out.add(s);
                    }
                }
            }
        }
    
        if (out.isEmpty() && !epsilon.getListen().getChannels().contains(channel)) {
            for (Set<State> partition : epsilonMap.get(epsilon)) {
                for (State s : p) {
                    if (s.canTakeInitiative(s.getOwner(), s, channel)) {
                        if (partition.contains(s.takeInitiative(s.getOwner(), s, channel).getDestination())) {
                            out.add(s);
                        }
                    }
                }
                if (!out.isEmpty() && !out.equals(p)) break;
                if (out.equals(p))                    out.clear();
            }
        }

        return out;
    }

    /**
     * Save the object to a file.
     * 
     * @param file File name where the data should be stored.
     * @return <code>true</code> if successful
     */
    public boolean saveToFile(String file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            try (ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(this);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Read object from a file.
     * 
     * @param file File name where the data is stored.
     * @return The object in the file.
     * @throws IOException If the file does not exist.
     */
    public static Smolka readFromFile(String file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            try (ObjectInputStream ois = new ObjectInputStream(fis)) {
                return (Smolka) ois.readObject();
            }
        } catch (ClassNotFoundException e) {
            // Should never happen
            e.printStackTrace();
            return null;
        }
    }
}
