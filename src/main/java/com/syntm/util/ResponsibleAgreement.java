package com.syntm.util;

import java.util.*;

import com.syntm.lts.State;
import com.syntm.lts.TS;

public class ResponsibleAgreement {

    public static Set<Set<State>> computeCliquePartition(TS ts, Map<String, Set<State>> S) {
       
        // Step 1: Greedy partition
        Set<State> unassigned = new HashSet<>(ts.getStates());
        Set<Set<State>> partition = new HashSet<>();

        while (!unassigned.isEmpty()) {
            State seed = unassigned.iterator().next();
            Set<State> clique = new HashSet<>();
            clique.add(seed);
          //  System.err.println("seed->"+seed.getId());
            for (State candidate : unassigned) {
                if (!candidate.equals(seed)
                        && agreesWithAll(candidate, clique, S)) {
                    
                    clique.add(candidate);
                   // System.err.println("seed->"+seed.getId()+" -> clique->"+ clique.toString());
                }
            }

            // Remove all members of the clique from unassigned
            unassigned.removeAll(clique);
            partition.add(clique);
        }

        return partition;
    }

    private static boolean agreesWithAll(State candidate, Set<State> clique, Map<String, Set<State>> S) {
        for (State member : clique) {
            if (!(S.containsKey(candidate.getId()) && S.containsKey(member.getId())
                    && S.get(candidate.getId()).contains(member)
                    && S.get(member.getId()).contains(candidate))) {
                return false;
            }
        }
        return true;
    }
}