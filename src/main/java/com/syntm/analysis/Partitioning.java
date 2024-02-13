package com.syntm.analysis;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.syntm.lts.Label;
import com.syntm.lts.State;
import com.syntm.lts.TS;


public class Partitioning {
    private TS T;
    private TS parameter;
    private ConcurrentHashMap<State, Set<Set<State>>> sMap;

    public Partitioning(TS parameter, TS T) {
        this.parameter = parameter;
        this.T = T;
        sMap = new ConcurrentHashMap<>();
    }

    public TS computeCompressedTS() {

        // Partition Locally Based On Label;

        Set<Set<State>> rho = new HashSet<>();
        rho.add(this.T.getStates());

        Set<Label> labs = new HashSet<>();
        for (State s : T.getStates()) {
            labs.add(s.getLabel());
        }
        for (Label l : labs) {
            Set<Set<State>> nonStablePartitions = new HashSet<>();

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
        Set<Set<State>> rhoInit = new HashSet<>();
        rhoInit.addAll(rho);
        for (State epsilon : this.parameter.getStates()) {
            sMap.put(epsilon, rhoInit);

        }
        Set<String> c = new HashSet<>(this.T.getInterface().getChannels());
        c.addAll(this.parameter.getInterface().getChannels());

        // ConcurrentSolver d= new ConcurrentSolver(sMap, this.T,c);
        // return d.run();

        // ESolver d= new ESolver(sMap, this.T,c);
        // SeqSolver d = new SeqSolver(sMap, this.T,c);
        
        Smolka d = new Smolka(sMap, this.T,c);
        return d.run();
    }

    private Set<State> findSplit(Set<State> p, Label label) {
        Set<State> out = new HashSet<>();
        for (State s : p) {
            if (s.getLabel().equals(label)) {
                out.add(s);
            }
        }
        return out;
    }

    private Set<Set<State>> split(Set<State> p, Label label) {
        Set<Set<State>> splitP = new HashSet<>();
        Set<State> splitter = findSplit(p, label);
        Set<State> notsplitter = new HashSet<>(p);
        notsplitter.removeAll(splitter);
        splitP.add(splitter);
        splitP.add(notsplitter);
        return splitP;
    }
}
