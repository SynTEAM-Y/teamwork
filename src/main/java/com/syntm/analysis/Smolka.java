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
import com.syntm.lts.CompressedTS;
import com.syntm.lts.State;
import com.syntm.lts.TS;
import com.syntm.lts.Trans;

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
    private TS transSystem;
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
            TS transSystem, Set<String> channels) {
        this.epsilonMap = new HashMap<>(epsilonMap);
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

        boolean changed = true;
        while (changed) {
            HashMap<State, Set<Set<State>>> rhoResults = new HashMap<>();
            for (State epsilon : this.epsilonMap.keySet()) {
                Queue<Set<State>> rhoWaiting = new LinkedList<>(epsilonMap.get(epsilon));
                Set<Set<State>> rhoTemp = new HashSet<>(epsilonMap.get(epsilon));
                while(!rhoWaiting.isEmpty()){
                    Set<State> partition = rhoWaiting.remove();
                    for (String ch : channels) { // O(c^2mn^2)
                        for (Trans trEpsilon : epsilon.getTrans()) { // O(cmn^2)
                            Set<Set<State>> ePartitions = new HashSet<>(this.epsilonMap.get(trEpsilon.getDestination()));
                            for (Set<State> ePrime : ePartitions) { // O(mn^2)
                                Set<State> splitter = applyEBisim(partition, trEpsilon, ePrime, ch,epsilon); // O(mn)
                                if (!splitter.isEmpty() && !splitter.equals(partition)) {
                                Set<Set<State>> splitP; // = new HashSet<>();
                                splitP = split(partition, splitter); // O(n) confirmed
                                rhoTemp.remove(partition);
                                rhoTemp.addAll(splitP);
                                rhoWaiting.addAll(splitP);
                                }
                            }
                        }
                    }
                }
                
                rhoResults.put(epsilon, rhoTemp);
                
            }
            changed = false;
            for (State e : rhoResults.keySet()) {
                if (!epsilonMap.get(e).equals(rhoResults.get(e))) {
                    // not a fixed point, repeat
                    changed = true;
                    epsilonMap.put(e, rhoResults.get(e));
                }
            }
        }

        Set<Set<State>> rhoFinal = new HashSet<>();
        for (State s : epsilonMap.keySet()) {
            // Gets the partition from the initial state.
            if (this.transSystem.initStateEqLabel(s)) {
                if (epsilonMap.get(s).size() == 1) return this.transSystem;
                rhoFinal.addAll(epsilonMap.get(s));
                break;
            } 
        }

        CompressedTS c = new CompressedTS("s-" + this.transSystem.getName());
        return c.compressedTS(this.transSystem, rhoFinal);
    }


    private Set<Set<State>> split(Set<State> p, Set<State> splitter) {
        Set<Set<State>> splitP = new HashSet<>();
        Set<State> notsplitter = new HashSet<>(p);
    
        notsplitter.removeAll(splitter); // O(n) confirmed by the internet
        splitP.add(splitter);
        splitP.add(notsplitter);
    
        return splitP;
      }

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
