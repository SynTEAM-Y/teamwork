package com.syntm.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.syntm.lts.CompressedTS;
import com.syntm.lts.State;
import com.syntm.lts.TS;
import com.syntm.lts.Trans;

/**
 * An object for reducing a transition system using the Kanellakis-Smolka
 * algorithm.
 */
public class Smolka {
    private Set<String> channels;
    private HashMap<State, Set<Set<State>>> epsilonMap;
    private TS transSystem;

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
    public Smolka(ConcurrentHashMap<State, Set<Set<State>>> indexedFamily,
            TS transSystem, Set<String> channels) {
        this.epsilonMap = new HashMap<>(indexedFamily);
        this.transSystem = transSystem;
        this.channels = channels;

    }

    /**
     * The method to call to run the Kanellakis-Smolka algorithm.
     * 
     * @return The reduced transition system.
     */
    public TS run() {
        Set<Set<State>> pi = epsilonMap.values().iterator().next(); // The initial partition

        Queue<Set<State>> pi_waiting = new LinkedList<>(pi);
        Set<Set<State>> pi_blocks = new HashSet<>(pi);

        while (pi_waiting.size() > 0) {
            Set<State> block = pi_waiting.remove();
            for (String channel : this.channels) {
                Set<Set<State>> splitP = smolkaSplit(block, pi_blocks, channel);
                if (splitP.size() > 1) {
                    pi_waiting.addAll(splitP);
                    // Replace the block with its subpartition.
                    pi_blocks.remove(block);
                    pi_blocks.addAll(splitP);
                }
            }
        }

        CompressedTS c = new CompressedTS("s-" + this.transSystem.getName());
        return c.compressedTS(this.transSystem, pi_blocks);
    }

    /**
     * The split function used by the Kanellakis-Smolka algorithm.
     * 
     * @param block   The splitter block
     * @param pi      The partition
     * @param channel The channel/action to split on
     * @return The split block(s)
     */
    private Set<Set<State>> smolkaSplit(Set<State> block, Set<Set<State>> pi, String channel) {
        Set<State> block1 = new HashSet<>();
        Set<State> block2 = new HashSet<>();
        State s = block.iterator().next();

        // Find the blocks with transitions from s.
        Set<Set<State>> sBlocks = new HashSet<>();
        for (Trans t : s.getTrans()) {
            if (t.getAction().equals(channel)) {
                for (Set<State> b : pi) {
                    if (b.contains(t.destination)) {
                        // Add the block in the partition that contains the destination state.
                        sBlocks.add(b);
                        break;
                    }
                }
            }
        }

        for (State t : block) {
            // Find the blocks with transitions from t and compare with sBlocks.

            Set<Set<State>> blockDestinationPartitions = new HashSet<>();
            Set<Trans> transitions = t.getTrans();
            for (Trans trans : transitions) {
                if (trans.getAction().equals(channel)) {
                    for (Set<State> b : pi) {
                        if (b.contains(trans.destination)) {
                            // Add the block in the partition that contains the destination state.
                            blockDestinationPartitions.add(b);
                            break;
                        }
                    }
                }
            }

            // if s and t can reach the same set of blocks in π via a-labelled transitions
            if (blockDestinationPartitions.equals(sBlocks)) {
                block1.add(t);
            } else {
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
}
