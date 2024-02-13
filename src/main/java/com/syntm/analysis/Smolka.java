package com.syntm.analysis;

import java.util.HashMap;
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
 * Estimated space complexity: O(c+nm+n^2)
 * <p>
 * Estimated time complexity: O(cmn^3)
 * <p>
 * (c channels, m transitions, n states)
 */
public class Smolka {
    private Set<String> channels;
    private HashMap<State, Set<Set<State>>> epsilonMap; // O(n^2+nm)
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
    public Smolka(ConcurrentMap<State, Set<Set<State>>> indexedFamily,
            TS transSystem, Set<String> channels) {
        this.epsilonMap = new HashMap<>(indexedFamily); // space complexity O(mn+n^2)?
        this.transSystem = transSystem;
        this.channels = channels; // Space complexity O(c)

    }

    /**
     * The method to call to run the Kanellakis-Smolka algorithm.
     * <p>
     * Time complexity: O(cmn^3)?
     * <p>
     * Space complexity: O(m+n)?
     * <p>
     * (c channels, m transitions, n states)
     * 
     * @return The reduced transition system.
     */
    public TS run() { // O(m+n) space
        Set<Set<State>> pi = epsilonMap.values().iterator().next(); // The initial partition

        Queue<Set<State>> piWaiting = new LinkedList<>(pi);
        Set<Set<State>> piBlocks = new HashSet<>(pi);

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

        // Find the blocks with transitions from s.
        Set<Set<State>> sBlocks = getDestinationBlocks(s, channel, pi);

        for (State t : block) { // O(n^3)
            Set<Set<State>> blockDestinationPartitions = getDestinationBlocks(t, channel, pi);

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

    /**
     * A method for getting the blocks pointed to by transitions from a state
     * <code>t</code>.
     * <p>
     * Time complexity: O(mn) or O(n^2)? 
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
    Set<Set<State>> getDestinationBlocks(State t, String channel, Set<Set<State>> pi) { // O(n^2) I think, n states
        Set<Set<State>> blockDestinationPartitions = new HashSet<>();
        Set<Trans> transitions = t.getTrans();
        for (Trans trans : transitions) { // Max n-1 (or should transitions [t] be used here?)
            if (trans.getAction().equals(channel)) {
                for (Set<State> b : pi) { // Max n
                    if (b.contains(trans.destination)) {
                        // Add the block in the partition that contains the destination state.
                        blockDestinationPartitions.add(b);
                        break;
                    }
                }
            }
        }
        return blockDestinationPartitions;
    }
}
