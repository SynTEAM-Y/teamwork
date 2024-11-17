package com.syntm.analysis;
/*
Author:  Yehia Abd Alrahman (yehiaa@chalmers.se)
SeqSolver.java (c) 2024
Desc: Sequential solver (uptodate)
Created:  17/11/2024 09:45:55
Updated:  17/11/2024 21:58:01
Version:  1.1
*/

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.javatuples.Pair;
import com.syntm.lts.CompressedTS;
import com.syntm.lts.State;
import com.syntm.lts.TS;
import com.syntm.lts.Trans;
import com.syntm.util.Printer;
public class SeqSolver {
    private class Task {
        State epsilon;
        Set<Set<State>> rho_epsilon;
        Set<Set<State>> rho_temp;
        Set<String> channels;
        HashMap<State, Set<Set<State>>> lMap;

        Task(String e, Set<Set<State>> rho_Set, HashMap<State, Set<Set<State>>> eMap,
                Set<String> channels) {
            this.channels = new HashSet<String>(channels);
            this.epsilon = eMap.keySet()
                    .stream()
                    .filter(s -> s.getId().equals(e))
                    .collect(Collectors.toSet()).iterator().next();

            this.rho_epsilon = new HashSet<>(rho_Set);

            this.rho_temp = new HashSet<>(rho_Set);
            this.lMap = new HashMap<>();
            for (Trans tr_e : epsilon.getTrans()) {
                Set<Set<State>> rho = new HashSet<>(eMap.get(tr_e.getDestination()));
                this.lMap.put(tr_e.getDestination(), rho);
            }

        }

        public void run() {

            for (String ch : channels) {
                List<Set<State>> rhoList = new ArrayList<>(this.rho_temp);
                for (Trans trEpsilon : epsilon.getTrans()) {
                    List<Set<State>> ePartitions = new ArrayList<Set<State>>(this.lMap.get(trEpsilon.getDestination()));
                    for (Set<State> ePrime : ePartitions) {
                        HashMap<Set<State>, Set<State>> splitters = new HashMap<>();
                        for (Set<State> partition : rhoList) {
                            if (partition.size() > 1) {
                                Set<State> splitter = applyEBisim(partition, trEpsilon, ePrime, ch);
                                if (!splitter.isEmpty() && !splitter.equals(partition)) {
                                    splitters.put(partition, splitter);
                                }
                            }
                        }
                        if (!splitters.isEmpty()) {
                            for (Set<State> p : splitters.keySet()) {
                                Set<Set<State>> splitP = split(p, splitters.get(p));
                                rhoList.remove(p);
                                rhoList.addAll(splitP);
                                rho_temp.remove(p);
                                rho_temp.addAll(splitP);
                            }
                        }
                    }
                }
            }

        }

        public State getEpsilon() {
            return epsilon;
        }

        public Set<Set<State>> getRho_epsilon() {
            return new HashSet<>(rho_epsilon);
        }

        public void setRho_epsilon(Set<Set<State>> rho_epsilon) {
            this.rho_epsilon = new HashSet<>(rho_epsilon);
        }

        public Set<Set<State>> getRho_temp() {
            return new HashSet<>(rho_temp);
        }

        public void setlMap(HashMap<State, Set<Set<State>>> lMap) {
            for (Trans tr_e : this.epsilon.getTrans()) {
                Set<Set<State>> rho = new HashSet<>(lMap.get(tr_e.getDestination()));
                this.lMap.put(tr_e.getDestination(), rho);
            }
        }

        private Set<State> applyEBisim(Set<State> p, Trans trEpsilon, Set<State> ePrime, String channel) {
            Set<State> out = new HashSet<State>();
            // Parameter invovled
            if (epsilon.enable(epsilon, channel)
                    && trEpsilon.getAction().equals(channel)) {

                if (epsilon.getOwner().getInterface().getChannels().contains(channel)) {
                    // reaction by s
                    out = sRacToE(p, ePrime, out, channel);
                }
                // initiation by s
                if (out.isEmpty()) {
                    out = sInitiateToE(p, ePrime, out, channel);
                }
            }

            // Parameter is not involved
            if (out.isEmpty() && !epsilon.getListen().getChannels().contains(channel)) {
                // Epsilon does not participate
                out = sInitiateAlone(p, out, channel);
            }

            return out;
        }

        private Set<State> sRacToE(Set<State> p, Set<State> ePrime, Set<State> out, String channel) {
            for (State s : p) {
                // 3.c
                if (s.canAnyReaction(s.getOwner(), s, channel)) {
                    if (ePrime.contains(s.takeAnyReaction(s.getOwner(), s,
                            channel).getDestination())) {
                        out.add(s);
                    }
                }
            }
            Set<State> pPrime = new HashSet<>();
            Set<State> outt = new HashSet<>(out);
            pPrime.addAll(p);
            pPrime.removeAll(out);
            if (!out.isEmpty()) {
                for (State s : pPrime) {
                    if (!s.canAnyReaction(s.getOwner(), s, channel)) {
                        if (!s.getListen().getChannels().contains(channel)) {
                            // Ahead of Epsilon
                            Set<State> ahead = outt
                                    .stream()
                                    .filter(h -> p.contains(h.takeAnyReaction(h.getOwner(), h,
                                            channel).getDestination()))
                                    .collect(Collectors.toSet());
                            if (!ahead.isEmpty()) {
                                out.add(s);
                            }

                            // Unordered / Before of Epsilon
                            if (!out.contains(s)) {
                                Set<State> witnessSet = new HashSet<>();
                                witnessSet = p.stream()
                                        .filter(w -> w.canAnyReaction(ts, w, channel) && !w.equals(s)
                                                && p.contains(w.takeAnyReaction(ts, w, channel).getDestination()))
                                        .collect(Collectors.toSet());
                                if (witnessSet.isEmpty()) {
                                    out.add(s);

                                }
                            }

                        }
                    }
                }
            }

            return out;
        }

        private Set<State> sInitiateToE(Set<State> p, Set<State> ePrime, Set<State> out, String channel) {
            if (!epsilon.canTakeInitiative(epsilon.getOwner(), epsilon, channel)
                    && epsilon.getListen().getChannels().contains(channel)) {
                for (State s : p) {
                    if (s.canTakeInitiative(s.getOwner(), s, channel)) {
                        if (ePrime.contains(s.takeInitiative(s.getOwner(), s, channel).getDestination())) {
                            out.add(s);
                        }
                    }
                }
            }
            return out;
        }

        private Set<State> sInitiateAlone(Set<State> p, Set<State> out, String channel) {
            for (Set<State> partition : rho_epsilon) { // all in same partition.
                for (State s : p) {
                    if (s.canTakeInitiative(s.getOwner(), s, channel)) {
                        if (partition.contains(s.takeInitiative(s.getOwner(), s, channel).getDestination())) {
                            out.add(s);
                        }
                    }
                }
                if (!out.isEmpty() && !out.equals(p)) { // split happen, exit
                    break;
                }
                if (out.equals(p)) { // no split, don't waste time
                    out.clear();
                }
            }
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

    static HashMap<State, Set<Set<State>>> eMap;

    public static void seteMap(HashMap<State, Set<Set<State>>> eMap) {
        SeqSolver.eMap = eMap;
    }

    private TS ts;
    private TS p;
    private Set<String> channels;
    List<Task> wList;

    public SeqSolver(HashMap<State, Set<Set<State>>> indexedFamily, TS t, TS p, Set<String> sch) {
        eMap = new HashMap<>(indexedFamily);
        this.channels = sch;
        this.ts = t;
        this.p = p;
        wList = new ArrayList<Task>();

        int i = 0;
        for (State s : eMap.keySet()) {
            Task w = new Task(s.getId(), eMap.get(s), eMap, channels);
            wList.add(i, w);
            i += i;
        }
    }

    public TS run() {
        // int counter = 0;
        boolean fixed = false;
        while (!fixed) {
            // counter += 1;
            // System.out.println("\n\n ROUND#" + counter + "\n\n");
            for (int i = 0; i < wList.size(); i++) {
                wList.get(i).run();
            }
            fixed = updateMap();
        }

        Set<Set<State>> rho_f = new HashSet<>();
        rho_f = buildPartition();

        CompressedTS c = new CompressedTS("s-" + this.ts.getName());
        TS t = c.DoCompress(this.ts, rho_f);
        return t;

    }

    private boolean updateMap() {
        boolean fixedPoint = true;

        for (int i = 0; i < wList.size(); i++) {
            if (!wList.get(i).getRho_epsilon().equals(wList.get(i).getRho_temp())) {
                wList.get(i).setRho_epsilon(wList.get(i).getRho_temp());
                eMap.put(wList.get(i).getEpsilon(), wList.get(i).getRho_temp());
                fixedPoint = false;
            }
        }
        // if (fixedPoint) {
        // printFixedRho(eMap);
        // }

        for (int i = 0; i < wList.size(); i++) {
            wList.get(i).setlMap(eMap);
        }

        return fixedPoint;
    }

    public Set<Set<State>> buildPartition() {
        Set<State> closed = new HashSet<>();
        Set<Set<State>> rho_intersect = new HashSet<>(eMap.get(p.getInitState()));

        HashMap<String, Set<State>> cares = new HashMap<>();

        Set<String> ids = new HashSet<>();
        ids = ts.getStates().stream().map(State::getId).collect(Collectors.toSet());

        for (State state : eMap.keySet()) {
            if (!state.equals(p.getInitState())) {
                rho_intersect.retainAll(eMap.get(state));
            }
        }

        for (String st : ids) {
            Set<State> states = eMap.get(p.getStateById(st)).stream().filter(p -> p.contains(ts.getStateById(st)))
                    .collect(Collectors.toSet()).iterator().next();
            cares.put(st, states);

        }

        if (rho_intersect.equals(eMap.get(p.getInitState()))) {
            return rho_intersect;
        }
        rho_intersect.clear();

        for (String s : ids) {
            if (!closed.contains(ts.getStateById(s))) {
                Set<State> b = new HashSet<>();
                b = cares.get(s);
                b.removeAll(closed);
                Set<State> excluded = new HashSet<>();
                List<Pair<State, Set<State>>> candidates = new ArrayList<>();
                for (State sPrime : b) {
                    if (!sPrime.getId().equals(s)) {
                        Set<State> pWise = new HashSet<>();
                        pWise.addAll(b);
                        pWise.retainAll(cares.get(sPrime.getId()));
                        if (pWise.contains(ts.getStateById(s))) {
                            candidates.add(new Pair<State, Set<State>>(sPrime, pWise));
                        }
                        if (!pWise.contains(ts.getStateById(s))) {
                            excluded.add(sPrime);
                        }
                    }
                }
                if (!excluded.isEmpty()) {
                    List<Pair<State, Set<State>>> cPW = new ArrayList<>(candidates);
                    for (Pair<State, Set<State>> p : cPW) {
                        Set<State> pExec = new HashSet<>();
                        pExec.addAll(p.getValue1());
                        pExec.retainAll(excluded);
                        if (!pExec.isEmpty()) {
                            candidates.remove(p);
                            p.getValue1().removeAll(pExec);
                            candidates.add(p);
                        }
                    }
                }
                if (candidates.isEmpty()) {
                    rho_intersect.add(new HashSet<>(Arrays.asList(ts.getStateById(s))));
                    closed.add(ts.getStateById(s));
                } else {
                    for (Pair<State, Set<State>> candidate : candidates) {
                        Set<State> disagree = new HashSet<>();
                        for (State fState : candidate.getValue1()) {
                            if (!fState.getId().equals(s) && !fState.getId().equals(candidate.getValue0().getId())) {
                                Set<State> inSet = new HashSet<>(candidate.getValue1());
                                inSet.retainAll(candidates.stream().filter(p -> p.getValue0().equals(fState))
                                        .collect(Collectors.toSet()).iterator().next().getValue1());
                                if (!inSet.equals(candidate.getValue1())) {
                                    disagree.add(fState);
                                }
                            }
                        }
                        candidate.getValue1().removeAll(disagree);
                    }
                    candidates.sort((e1, e2) -> e2.getValue1().size() - e1.getValue1().size());
                    rho_intersect.add(candidates.get(0).getValue1());
                    closed.addAll(candidates.get(0).getValue1());
                }
            }
        }
        // System.err.println("rho final" + rho_intersect);
        return rho_intersect;
    }

    public void printFixedRho(HashMap<State, Set<Set<State>>> map) {
        Printer gp = new Printer(this.ts.getName() + "'s Fixed Rho");
        gp.addln("\n An e-Cooperative Bisimulation for " + this.ts.getName() + "\n");
        List<State> keys = new ArrayList<State>();

        for (State epsilon : map.keySet()) {
            keys.add(epsilon);
        }
        keys.sort((e1, e2) -> e1.getId().compareTo(e2.getId()));

        for (State epsilon : keys) {
            gp.addln("\t" + epsilon.getId() + " ->");
            for (Set<State> set : map.get(epsilon)) {
                gp.addln("\t\t" + set);
            }
            gp.addln("\n\t" + "post-> " + epsilon.getPost() + "\n");
            gp.addln("\t" + "pre-> " + epsilon.getPre() + "\n");

        }
        gp.printText();
    }
}
