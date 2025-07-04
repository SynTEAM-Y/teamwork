package com.syntm.analysis;
/*
Author:  Yehia Abd Alrahman (yehiaa@chalmers.se)
SeqSolver.java (c) 2024
Desc: Sequential solver (uptodate)
Created:  17/11/2024 09:45:55
Updated:  05/07/2025 00:08:03
Version:  1.1
*/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.syntm.lts.CompressedTS;
import com.syntm.lts.State;
import com.syntm.lts.TS;
import com.syntm.lts.Trans;
import com.syntm.util.Printer;
import com.syntm.util.ResponsibleAgreement;
import com.syntm.util.ResponsibleAgreementBitSet;

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
                // 3.b (i)
                if (s.canAnyReaction(s.getOwner(), s, channel)) {
                    if (ePrime.contains(s.takeAnyReaction(s.getOwner(), s,
                            channel).getDestination())) {
                        out.add(s);
                    }
                }
            }
            // 3.b (ii)
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
                                Set<State> ownEpsilon = out.stream()
                                        .filter(st -> st.getId().equals(this.epsilon.getId()))
                                        .collect(Collectors.toSet());
                                if (!ownEpsilon.isEmpty()) {
                                    Set<State> witnessSet = new HashSet<>();
                                    witnessSet = p.stream()
                                            .filter(w -> w.canAnyReaction(ts, w, channel) && !w.equals(s)
                                                    && p.contains(w.takeAnyReaction(ts, w, channel).getDestination()))
                                            .collect(Collectors.toSet());

                                    if (witnessSet.isEmpty()) {
                                        out.add(s);

                                    }
                                    // else {

                                    // for (State state : witnessSet) {
                                    // Set<String> cSet = new HashSet<>();
                                    // cSet.addAll(s.getCh());
                                    // cSet.retainAll(state.getCh());
                                    // if (!cSet.isEmpty()) {
                                    // for (String rCH : cSet) {
                                    // if (p.contains(s.takeAnyReaction(ts, s, rCH).getDestination())) {
                                    // if (!p.contains(state.takeAnyReaction(ts, state, rCH)
                                    // .getDestination())) {
                                    // out.add(s);
                                    // return out;
                                    // }
                                    // } else {
                                    // if (p.contains(state.takeAnyReaction(ts, state, rCH)
                                    // .getDestination())) {
                                    // out.add(s);
                                    // return out;
                                    // }
                                    // }
                                    // }

                                    // }
                                    // }

                                    // }
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
            i++;
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
        this.ts.toDotPartition(rho_f);
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
        //     printFixedRho(eMap);
        // }

        for (int i = 0; i < wList.size(); i++) {
            wList.get(i).setlMap(eMap);
        }

        return fixedPoint;
    }

    public Set<Set<State>> buildPartition() {
        HashMap<String, Set<State>> cares = new HashMap<>();
        Set<String> ids = new HashSet<>();
        ids = ts.getStates().stream().map(State::getId).collect(Collectors.toSet());

        Set<Set<State>> rho_intersect = new HashSet<>(eMap.get(p.getInitState()));
        for (State state : eMap.keySet()) {
            if (!state.equals(p.getInitState())) {
                rho_intersect.retainAll(eMap.get(state));
            }
        }
        if (rho_intersect.equals(eMap.get(p.getInitState()))) {
            return rho_intersect;
        }

        for (String st : ids) {
            Set<State> states = eMap.get(p.getStateById(st)).stream().filter(p -> p.contains(ts.getStateById(st)))
                    .collect(Collectors.toSet()).iterator().next();
            cares.put(st, states);

        }

        // Set<Set<State>> partition = ResponsibleAgreement.computeCliquePartition(ts,
        // cares);
        Set<Set<State>> partition = ResponsibleAgreement.computeCliquePartition(ts, this.preProcess(cares, eMap));
        // Set<Set<State>> partition = ResponsibleAgreement.computeCliquePartition(ts,
        // cares);

        return partition;

    }

    private Set<State> yPreC(String y, Set<State> C) {
        Set<State> temp = new HashSet<>();
        temp = C.stream()
                .flatMap(s -> s.getyPre(y).stream())
                .collect(Collectors.toSet());

        return temp;
    }

    private HashMap<String, Set<State>> preProcess(HashMap<String, Set<State>> cares,
            HashMap<State, Set<Set<State>>> eMap) {
        HashMap<String, Set<State>> tempCare = new HashMap<>(cares);
        for (String id : new HashSet<>(cares.keySet())) {
            for (State s : new HashSet<>(cares.get(id))) {
                if (!s.equals(ts.getStateById(id)) && cares.get(s.getId()).contains(ts.getStateById(id))) {
                    for (String y : ts.getInterface().getChannels()) {
                        Set<State> preCminusS = new HashSet<>(cares.get(id));
                        preCminusS.remove(s);
                        List<State> preC = new ArrayList<>(this.yPreC(y, preCminusS));
                        if (violatesAgreement(id, s, y, preC,
                                tempCare)) {
                            tempCare.get(id).remove(s);
                            tempCare.get(s.getId()).remove(ts.getStateById(id));
                            break;
                        }

                    }
                }
            }

        }
        return tempCare;
    }

    private boolean violatesAgreement(String id, State s, String y, List<State> preC,
            Map<String, Set<State>> tempCare) {
        for (int i = 0; i < preC.size(); i++) {
            for (int j = i + 1; j < preC.size(); j++) {
                State s1 = preC.get(i), s2 = preC.get(j);
                State p1 = s1.getyPost(y), p2 = s2.getyPost(y);
                if (p1 == null || p2 == null)
                    continue;
                if (tempCare.getOrDefault(s1.getId(), Set.of()).contains(s2)
                        && tempCare.getOrDefault(s2.getId(), Set.of()).contains(s1)) {
                    if (!tempCare.getOrDefault(p1.getId(), Set.of()).contains(s)
                            || !tempCare.getOrDefault(p2.getId(), Set.of()).contains(s)
                            || !tempCare.getOrDefault(s.getId(), Set.of()).contains(p1)
                            || !tempCare.getOrDefault(s.getId(), Set.of()).contains(p2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void printFixedRho(HashMap<State, Set<Set<State>>> map) {
        HashMap<String, Set<State>> cares = new HashMap<>();
        Set<String> ids = new HashSet<>();
        ids = ts.getStates().stream().map(State::getId).collect(Collectors.toSet());

        for (String st : ids) {
            Set<State> states = eMap.get(p.getStateById(st)).stream().filter(p -> p.contains(ts.getStateById(st)))
                    .collect(Collectors.toSet()).iterator().next();
            cares.put(st, states);

        }
        Printer gp = new Printer(this.ts.getName() + "'s Fixed Rho");
        gp.addln("\n An e-Cooperative Bisimulation for " + this.ts.getName() + "\n");

        List<String> skeys = new ArrayList<String>();
        for (String id : cares.keySet()) {
            skeys.add(id);
        }
        skeys.sort((e1, e2) -> e1.compareTo(e2));
        for (String idString : skeys) {
            gp.addln("\t" + idString + " ->");
            for (State set : cares.get(idString)) {
                gp.addln("\t\t" + set);
            }
            // gp.addln("\n\t" + "post-> " + ts.getStateById(idString).getPost() + "\n");
            // gp.addln("\t" + "pre-> " + ts.getStateById(idString).getPre() + "\n");

        }
        gp.printText();

    }
}
