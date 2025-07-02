package com.syntm.analysis;
/*
Author:  Yehia Abd Alrahman (yehiaa@chalmers.se)
Partitioning.java (c) 2024
Desc: Initial lebel partitioning and pass to a specific solver
Created:  17/11/2024 09:45:55
Updated:  23/06/2025 00:51:06
Version:  1.1
*/
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.stream.Collectors;
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

        Set<Set<State>> rho = new HashSet<Set<State>>();
        rho.add(this.T.getStates());

        Set<Label> labs = new HashSet<Label>();
        for (State s : T.getStates()) {
            labs.add(s.getLabel());
        }
        for (Label l : labs) {
            HashMap<Set<State>, Set<State>> nonStablePartitions = new HashMap<>();

            for (Set<State> partition : rho) {
                if (partition.size() > 1) {
                    Set<State> splitter = findSplit(partition, l);
                    if (!splitter.isEmpty() && !splitter.equals(partition)) {
                        nonStablePartitions.put(partition, splitter);
                    }
                }
            }
            if (!nonStablePartitions.isEmpty()) {
                for (Set<State> p : nonStablePartitions.keySet()) {
                    Set<Set<State>> splitP = split(p, nonStablePartitions.get(p));
                    rho.remove(p);
                    rho.addAll(splitP);
                }
            }
        }
        Set<Set<State>> rhoInit = new HashSet<Set<State>>();
        HashMap<State, Set<Set<State>>> hMap = new HashMap<>();
        rhoInit.addAll(rho);
        for (State epsilon : this.Parameter.getStates()) {
            sMap.put(epsilon, rhoInit);
            hMap.put(epsilon, rhoInit);

        }
        Set<String> c = new HashSet<>(this.T.getInterface().getChannels());
        c.addAll(this.Parameter.getInterface().getChannels());

        //System.out.println("TS to be minimised -> " + this.T.getName());// +" with "+this.T.getInterface());
         // ESolver d = new ESolver(sMap, this.T, this.Parameter, c);
        //  return d.run();

        // ConcurrentSolver d= new ConcurrentSolver(sMap, this.T, this.Parameter,c);
        // return d.run();

        SeqSolver d= new SeqSolver(hMap, this.T, this.Parameter,c);
        return d.run();
    }

    private Set<State> findSplit(Set<State> p, Label label) {
        Set<State> out = new HashSet<State>();
        out = p
                .stream()
                .filter(s -> s.getLabel().equals(label))
                .collect(Collectors.toSet());
        // for (State s : p) {
        // if (s.getLabel().equals(label)) {
        // out.add(s);
        // }
        // }
        return out;
    }

    private Set<Set<State>> split(Set<State> p, Set<State> splitter) {
        Set<Set<State>> splitP = new HashSet<Set<State>>();
        Set<State> notsplitter = new HashSet<State>(p);
        notsplitter.removeAll(splitter);

        splitP.add(splitter);

        splitP.add(notsplitter);
        return splitP;
    }

}
