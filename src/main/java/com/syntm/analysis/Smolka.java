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
        this.initPartition = indexedFamily.values().iterator().next(); // space complexity O(m+n)?
        this.lastId = 0;
        for (Set<State> b : initPartition) {
            for (State t : b) { 
                t.setBlockId(this.lastId);
            }
            this.lastId++;
        }
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
    public TS run() { // O(m+n) space
        Queue<Set<State>> piWaiting = new LinkedList<>(this.initPartition);
        Set<Set<State>> piBlocks = new HashSet<>(this.initPartition);

        while (!piWaiting.isEmpty()) { // Max n -> O(cmn)
            Set<State> block = piWaiting.remove();
            for (String channel : this.channels) { // O(cm)
                Set<Set<State>> splitP = smolkaSplit(block, channel); // O(m)
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
     * Time complexity: O(m)?
     * <p>
     * Space complexity: O(m+n)?
     * <p>
     * (m transitions, n states)
     * 
     * @param block   The splitter block
     * @param channel The channel/action to split on
     * @return The split block(s)
     */
    private Set<Set<State>> smolkaSplit(Set<State> block, String channel) { // O(m)
        
        Set<State> block1 = new HashSet<>();
        Set<State> block2 = new HashSet<>();
        State s = block.iterator().next();

        // Find the blocks with transitions from s.
        Set<Integer> sBlocks = getDestinationBlocks(s, channel); // O(m)
        int newId = getNewId();

        for (State t : block) { // Each transition is only checked once, so this becomes O(m)
            Set<Integer> blockDestinationPartitions = getDestinationBlocks(t, channel);
            // if s and t can reach the same set of blocks in π via a-labelled transitions
            if (blockDestinationPartitions.equals(sBlocks)) {
                block1.add(t);
            } else {
                t.setBlockId(newId);
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

    /**
     * A method for getting the blocks pointed to by transitions from a state
     * <code>t</code>.
     * 
     * @param t       The origin state
     * @param channel The channel/action to follow
     * @param pi      The main partition
     * @return The set of blocks pointed to
     */
    private Set<Integer> getDestinationBlocks(State t, String channel) { // O(m)
        Set<Integer> blockDestinationPartitions = new HashSet<>();
        Set<Trans> transitions = t.getTrans();
        for (Trans trans : transitions) { // O(m)
            if (trans.getAction().equals(channel)) {
                blockDestinationPartitions.add(trans.destination.getBlockId()); // O(1)
            }
        }
        return blockDestinationPartitions;
    }

    private int getNewId() {
        return ++this.lastId;
    }
}
