package com.syntm.util;

import java.util.Set;

import com.syntm.lts.State;
import com.syntm.lts.TS;

import java.util.HashSet;

public class AgreeComputation {

    TS t;

    

    public AgreeComputation(TS t) {
        this.t = t;
    }

    // Your function f(s)
    State f(State s) {
        // TODO: return parameter for s
        return null;
    }

    // Returns the equivalence class [s]^{~_{f(s)}} as a set of States
    Set<State> getEquivalenceClass(State s, State param) {
        // TODO: implement this based on your equivalence relation
        return new HashSet<>();
    }

    // Returns the set of predecessors of a set of states Pre(X)
    Set<State> getPredecessors(Set<State> states) {
        // TODO: implement
        return new HashSet<>();
    }

    // Returns true if iReach(s,s') holds
    boolean isIReach(State s, State sPrime) {
        // TODO: implement reachability check
        return true;
    }

    // Main Agree computation method
    public boolean agree(State s, State sPrime) {
        State f_s = f(s);
        State f_sPrime = f(sPrime);

        Set<State> class_sPrime = getEquivalenceClass(sPrime, f_sPrime);
        Set<State> class_s = getEquivalenceClass(s, f_s);

        // Check s in [s']^{sim_f(s')} and s' in [s]^{sim_f(s)}
        if (!class_sPrime.contains(s) || !class_s.contains(sPrime)) {
            return false;
        }

        if (!isIReach(s, sPrime)) {
            // The implication is true if premise false, so agree is true here
            return true;
        }

        // Now we check the two big conditions:

        // Predecessors of classes
        Set<State> pre_class_s = getPredecessors(class_s);
        Set<State> pre_class_sPrime = getPredecessors(class_sPrime);

        // First big forall condition:
        for (State s1 : pre_class_s) {
            for (State s2 : pre_class_s) {
                if (getEquivalenceClass(s1, f(s1)).contains(s2) &&
                    getEquivalenceClass(s2, f(s2)).contains(s1)) {
                    for (State s3 : pre_class_sPrime) {
                        if (!(getEquivalenceClass(s3, f(s3)).contains(s1) &&
                              getEquivalenceClass(s1, f(s1)).contains(s3) &&
                              getEquivalenceClass(s3, f(s3)).contains(s2) &&
                              getEquivalenceClass(s2, f(s2)).contains(s3))) {
                            return false;
                        }
                    }
                }
            }
        }

        // Second big forall condition:
        for (State s1 : pre_class_sPrime) {
            for (State s2 : pre_class_sPrime) {
                if (getEquivalenceClass(s1, f(s1)).contains(s2) &&
                    getEquivalenceClass(s2, f(s2)).contains(s1)) {
                    for (State s3 : pre_class_s) {
                        if (!(getEquivalenceClass(s3, f(s3)).contains(s1) &&
                              getEquivalenceClass(s1, f(s1)).contains(s3) &&
                              getEquivalenceClass(s3, f(s3)).contains(s2) &&
                              getEquivalenceClass(s2, f(s2)).contains(s3))) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }
}
