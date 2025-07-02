package com.syntm.util;

import java.util.*;
import java.util.stream.Collectors;

import com.syntm.lts.State;
import com.syntm.lts.TS;

public class ResponsibleAgreementBitSet {

    // Computes a partition of states into mutual agreement cliques
    public static Set<Set<State>> computeCliquePartition(TS ts, Map<String, Set<State>> S) {
        var states = new ArrayList<>(ts.getStates()); // Get all states from the transition system
        var n = states.size(); // Number of states

        // Map from state ID to index for fast lookup
        var idToIndex = new HashMap<String, Integer>();
        // Array for index-to-State reverse lookup
        var indexToState = new State[n];
        for (var i = 0; i < n; i++) {
            var s = states.get(i);
            idToIndex.put(s.getId(), i); // Map ID to index
            indexToState[i] = s; // Store state in index array
        }

        var G = createBitSetArray(n); // Adjacency BitSet array for mutual agreement graph

        // Build the symmetric agreement graph G
        for (var i = 0; i < n; i++) {
            var si = indexToState[i];
            var id1 = si.getId();
            if (!S.containsKey(id1))
                continue; // Skip if state has no agreement entry
            for (var sj : S.get(id1)) { // For each state sj that si agrees with
                var id2 = sj.getId();
                var j = idToIndex.get(id2); // Get index of sj
                // Check mutual agreement: si agrees with sj and sj agrees with si
                if (j != null && S.containsKey(id2) && S.get(id2).contains(si)) {
                    G[i].set(j); // Set G[i][j] = true
                }
            }
        }

        var unassigned = new BitSet(n); // Tracks which states are not yet assigned to a clique
        unassigned.set(0, n); // Initially all states are unassigned
        var partition = new HashSet<Set<State>>(); // Final partition (set of cliques)

        // Extract cliques greedily from unassigned nodes
        while (!unassigned.isEmpty()) {
            var seed = unassigned.nextSetBit(0); // Pick the first unassigned state
            var clique = (BitSet) G[seed].clone(); // Start with its neighbors
            clique.and(unassigned); // Keep only unassigned neighbors
            //System.err.println("seed -> "+ G[seed]);
          //  clique.set(seed); // Include the seed itself
            //  System.err.println("Before and loop seed->"+indexToState[seed].getId()+" -> clique->"+ clique.toString());
            // Prune the tentative clique to ensure all members agree with each other
            for (var i = clique.nextSetBit(0); i >= 0; i = clique.nextSetBit(i + 1)) {
                var neighbors = (BitSet) G[i].clone(); // Get neighbors of i
                neighbors.and(clique); // Restrict to current clique
                //System.err.println(neighbors.toString());
                neighbors.set(i); // Include self

                // System.err.println("Before seed->"+indexToState[seed].getId()+" -> clique->"+ clique.toString()+" cand-> "+indexToState[i].getId()+" care-> "+G[i]);

                if (!neighbors.equals(clique))
                    clique.clear(i); // Remove i if not fully connected
                // System.err.println("after seed->"+indexToState[seed].getId()+" -> clique->"+ clique.toString()+" cand-> "+indexToState[i].getId());
                
                // if (ts.getStateById(indexToState[seed].getId()).isReachInitiate() && i!=seed) {
                //     //System.err.println("seed->"+indexToState[seed].getId()+" -> cand->"+indexToState[i].getId());
                //     Set<String> idsSeed = new HashSet<>();
                //     idsSeed = ts.getStateById(indexToState[seed].getId()).getPre().stream().map(State::getId)
                //             .collect(Collectors.toSet());

                //   //  System.err.println("idsSeed->"+idsSeed.toString());

                //     BitSet bitSetSeed = new BitSet();
                //     for (String id : idsSeed) {
                //         bitSetSeed.set(idToIndex.get(id));
                //     }

                //     Set<String> idsN = new HashSet<>();
                //     idsN = ts.getStateById(indexToState[i].getId()).getPre().stream().map(State::getId).collect(Collectors.toSet());
                    
                //     // System.err.println("idsN->"+idsN.toString());

                //     BitSet bitSetN = new BitSet();
                //     for (String id : idsN) {
                //         bitSetN.set(idToIndex.get(id));
                //     }
                //         boolean disagreeFound = false;
                //         for (int p = bitSetSeed.nextSetBit(0); p >= 0
                //                 && !disagreeFound; p = bitSetSeed.nextSetBit(p + 1)) {
                //             for (int q = bitSetN.nextSetBit(0); q >= 0; q = bitSetN.nextSetBit(q + 1)) {
                //                 // Check mutual agreement between p and q
                //                 if (!G[p].get(q) || !G[q].get(p)) {
                //                     disagreeFound = true;
                //                     break;
                //                 }
                //             }
                //         }
                      
                //         if (disagreeFound) {
                //             clique.clear(i); // Remove i if any disagreement found
                //         }
                // }
            }

            // System.err.println("i exited");

            var group = new HashSet<State>(); // The actual clique to be added
            for (var i = clique.nextSetBit(0); i >= 0; i = clique.nextSetBit(i + 1)) {
                group.add(indexToState[i]); // Add state to group
                unassigned.clear(i); // Mark it as assigned
            }

            partition.add(group); // Add the clique to the partition
        }

        return partition; // Return the final partition of agreement cliques
    }

    // Utility: create an array of BitSets, each of size n
    private static BitSet[] createBitSetArray(int n) {
        var array = new BitSet[n];
        for (var i = 0; i < n; i++)
            array[i] = new BitSet(n);
        return array;
    }
}
