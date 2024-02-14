package com.syntm.analysis;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

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
 * Estimated time complexity: O(cmn^3)
 * <p>
 * (c channels, m transitions, n states)
 */
public class Smolka {
    private Set<String> channels;
    private Set<Set<State>> initPartition;
    private TS transSystem;
    private int lastId;

    /**
     * Contructor for a class using the Kanellakis-Smolka algorithm to reduce the
     * transition system.
     * 
     * @param indexedFamily A concurrent hashmap with an initial partition mapped to
     *                      each state.
     * @param transSystem   The transition system to be reduced.
     * @param channels      A set of the available channels.
     * @return An object callable with <code>run()</code> to reduce the transition
     *         system using the Kannelakis-Smolka algorithm.
     */
    public Smolka(ConcurrentMap<State, Set<Set<State>>> indexedFamily,
            TS transSystem, Set<String> channels) {
        System.out.println("New constructor!!!!!!!!");
        this.initPartition = indexedFamily.values().iterator().next(); // space complexity O(m+n)?
        this.lastId = 0;
        // Only 1 block in initPartition
        for (State t : initPartition.iterator().next()) { 
            t.setPartitionId(lastId);
        }
        this.transSystem = transSystem;
        this.channels = channels; // Space complexity O(c)

    }

    /**
     * The method to call to run the Kanellakis-Smolka algorithm.
     * <p>
     * Time complexity: O(cmn^3)?
     * <p>
     * Space complexity: O(c+m+n)?
     * <p>
     * (c channels, m transitions, n states)
     * 
     * @return The reduced transition system.
     */
    public TS run() { // O(m+n) space
        Queue<Set<State>> piWaiting = new LinkedList<>(this.initPartition);
        Set<Set<State>> piBlocks = new HashSet<>(this.initPartition);

        while (!piWaiting.isEmpty()) { // Max n -> O(cn^4), maybe O(cmn^3)
            Set<State> block = piWaiting.remove();
            for (String channel : this.channels) { // O(cn^3), n states, c channels
                Set<Set<State>> splitP = smolkaSplit(block, piBlocks, channel);
                if (splitP.size() > 1) {
                    piWaiting.addAll(splitP);
                    // Replace the block with its subpartition.
                    piBlocks.remove(block);
                    piBlocks.addAll(splitP);
                }
            }
        }

        CompressedTS c = new CompressedTS("s-" + this.transSystem.getName());
        return c.compressedTS(this.transSystem, piBlocks);
    }

    /**
     * The split function used by the Kanellakis-Smolka algorithm.
     * <p>
     * Time complexity: O(mn^2)?
     * <p>
     * Space complexity: O(m+n)?
     * <p>
     * (m transitions, n states)
     * 
     * @param block   The splitter block
     * @param pi      The partition
     * @param channel The channel/action to split on
     * @return The split block(s)
     */
    private Set<Set<State>> smolkaSplit(Set<State> block, Set<Set<State>> pi, String channel) { // O(n^3)
        Set<State> block1 = new HashSet<>();
        Set<State> block2 = new HashSet<>();
        State s = block.iterator().next();
        Set<Integer> tttt = new HashSet<>();

        // Find the blocks with transitions from s.
        Set<Integer> sBlocks = getDestinationBlocks(s, channel, pi);
        int newId1 = getNewId();
        int newId2 = getNewId();


        for (State t : block) { // O(n^3)
            Set<Integer> blockDestinationPartitions = getDestinationBlocks(t, channel, pi);
            tttt.add(t.getPartitionId());
            // if s and t can reach the same set of blocks in π via a-labelled transitions
            // System.out.println("state partitions");
            // System.out.println(sBlocks);
            // System.out.println(blockDestinationPartitions);
            // System.out.println(blockDestinationPartitions.equals(sBlocks));
            if (blockDestinationPartitions.equals(sBlocks)) {
                t.setPartitionId(newId1);
                block1.add(t);
            } else {
                t.setPartitionId(newId2);
                block2.add(t);
            }
        }
        System.out.println("Num of partition ids: " + tttt.size());

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

    /**
     * A method for getting the blocks pointed to by transitions from a state
     * <code>t</code>.
     * <p>
     * Time complexity: O(mn)? 
     * <p>
     * Space complexity: O(m+n)?
     * <p>
     * (m transitions, n states)
     * 
     * @param t       The origin state
     * @param channel The channel/action to follow
     * @param pi      The main partition
     * @return The set of blocks pointed to
     */
    private Set<Integer> getDestinationBlocks(State t, String channel, Set<Set<State>> pi) { // O(mn) I think
        Set<Integer> blockDestinationPartitions = new HashSet<>();
        Set<Set<State>> partitions = new HashSet<>();
        Set<Trans> transitions = t.getTrans();
        for (Trans trans : transitions) { // O(m)
            if (trans.getAction().equals(channel)) {
                for (Set<State> b : pi) { // Max n -> O(n)
                    if (b.contains(trans.destination)) {
                        // Add the block in the partition that contains the destination state.
                        partitions.add(b);
                        break;
                    }
                }
                // System.out.println(trans.destination.getPartitionId());
                blockDestinationPartitions.add(trans.destination.getPartitionId()); // O(1)
            }
        }
        // System.out.println("getDestinationBlocks");
        // System.out.println(partitions);
        // System.out.println(blockDestinationPartitions);
        return blockDestinationPartitions;
    }

    private int getNewId() {
        return ++this.lastId;
    }
}
