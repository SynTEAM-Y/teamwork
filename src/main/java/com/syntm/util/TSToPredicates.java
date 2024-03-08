package com.syntm.util;

import java.util.HashSet;
import java.util.Set;

import com.syntm.lts.Label;
import com.syntm.lts.State;
import com.syntm.lts.TS;
import com.syntm.lts.Trans;

public class TSToPredicates {
    private Set<Trans> transitions;
    private Set<Label> allLabels;
    private Set<String> allChannels;

    
    public TSToPredicates(TS inputTS) {
        this.transitions = inputTS.getTransitions();
        this.allChannels = inputTS.getInterface().getChannels();
        this.allLabels   = new HashSet<>();
        for (State s : inputTS.getStates()) {
            this.allLabels.add(s.getLabel());
        }
    }

    public static String toPredicates(TS ts) {
        return ts.toString();
    }
}
