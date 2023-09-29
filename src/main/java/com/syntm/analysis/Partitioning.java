package com.syntm.analysis;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.syntm.lts.Label;
import com.syntm.lts.State;
import com.syntm.lts.TS;


public class Partitioning {
    private TS T;
    private TS Parameter;
    private ConcurrentHashMap<State, Set<Set<State>>> sMap;

    public Partitioning(TS Parameter, TS T) {
        this.Parameter = Parameter;
        this.T = T;
        sMap = new ConcurrentHashMap<>();
    }

    public TS computeCompressedTS() {

        // partition locally based on label;

        Set<Set<State>> rho = new HashSet<Set<State>>();
        rho.add(this.T.getStates());

        Set<Label> labs = new HashSet<Label>();
        for (State s : T.getStates()) {
            labs.add(s.getLabel());
        }
        for (Label l : labs) {
            Set<Set<State>> nonStablePartitions = new HashSet<Set<State>>();

            for (Set<State> partition : rho) {
                Set<State> splitter = findSplit(partition, l);
                if (!splitter.isEmpty() && !splitter.equals(partition)) {
                    nonStablePartitions.add(partition);
                }
            }

            for (Set<State> p : nonStablePartitions) {
                Set<Set<State>> splitP = split(p, l);
                rho.remove(p);
                rho.addAll(splitP);
            }

        }
        Set<Set<State>> rhoInit = new HashSet<Set<State>>();
        rhoInit.addAll(rho);
        for (State epsilon : this.Parameter.getStates()) {
            sMap.put(epsilon, rhoInit);

        }
        Set<String> c = new HashSet<>(this.T.getInterface().getChannels());
        c.addAll(this.Parameter.getInterface().getChannels());

        ConcurrentSolver d= new ConcurrentSolver(sMap, this.T,c);
        return d.run();
    }

    private Set<State> findSplit(Set<State> p, Label label) {
        Set<State> out = new HashSet<State>();
        for (State s : p) {
            if (s.getLabel().equals(label)) {
                out.add(s);
            }
        }
        return out;
    }

    private Set<Set<State>> split(Set<State> p, Label label) {
        Set<Set<State>> splitP = new HashSet<Set<State>>();
        Set<State> splitter = findSplit(p, label);
        Set<State> notsplitter = new HashSet<State>(p);
        notsplitter.removeAll(splitter);
        splitP.add(splitter);
        splitP.add(notsplitter);
        return splitP;
    }
}
