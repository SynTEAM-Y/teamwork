package com.syntm.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
            // for (State state : eMap.keySet()) {
            // if (state.getId().equals(e)) {
            // this.epsilon = state;
            // break;
            // }
            // }
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
                for (Trans trEpsilon : epsilon.getTrans()) {
                    Set<Set<State>> ePartitions = new HashSet<>(this.lMap.get(trEpsilon.getDestination()));
                    for (Set<State> ePrime : ePartitions) {
                        HashMap<Set<State>, Set<State>> splitters = new HashMap<>();
                        for (Set<State> partition : this.rho_temp) {
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
                    out = sAnyToE(p, ePrime, out, channel);
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

        private Set<State> sAnyToE(Set<State> p, Set<State> ePrime, Set<State> out, String channel) {
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
            pPrime.addAll(p);
            pPrime.removeAll(out);
            if (!out.isEmpty()) {
                for (State s : pPrime) {
                    if (!s.canAnyReaction(s.getOwner(), s, channel)) {
                        if (!s.getListen().getChannels().contains(channel)) {
                            {
                                out.add(s);
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
    private Set<String> channels;
    List<Task> wList;

    public SeqSolver(HashMap<State, Set<Set<State>>> indexedFamily, TS t, Set<String> sch) {
        eMap = new HashMap<>(indexedFamily);
        this.channels = sch;
        this.ts = t;
        wList = new ArrayList<Task>();

        int i = 0;
        for (State s : eMap.keySet()) {
            Task w = new Task(s.getId(), eMap.get(s), eMap, channels);
            wList.add(i, w);
            i += i;
        }
    }

    public TS run() {
        //int counter = 0;
        boolean fixed = false;
        while (!fixed) {
          //  counter += 1;
            // System.out.println("\n\n SYNCHRONISATION ROUND#" + counter + "\n\n");
            for (int i = 0; i < wList.size(); i++) {
                wList.get(i).run();
            }
            fixed = updateMap();
        }

        Set<Set<State>> rho_f = new HashSet<>();
        String id = ts.getInitState().getId();
        for (State s : eMap.keySet()) {
            if (s.getId().equals(id)) {
                rho_f = new HashSet<>(eMap.get(s));
            }
        }

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
        if (fixedPoint) {
            printFixedRho(eMap);
        }

        for (int i = 0; i < wList.size(); i++) {
            wList.get(i).setlMap(eMap);
        }

        return fixedPoint;
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
        gp.addln("\n === Care of Epsilons === \n");

        wList.sort((e1, e2) -> e1.getEpsilon().getId().compareTo(e2.getEpsilon().getId()));

        for (Task t : wList) {

            final Set<State> temp = t.lMap.keySet()
                    .stream()
                    .filter(s -> s.getId() != t.getEpsilon().getId()).collect(Collectors.toSet());
            // System.err.println("temp ->" + temp);

            Set<String> ids = temp.stream().map(State::getId).collect(Collectors.toSet());

            ids.add(t.getEpsilon().getId());

            // System.err.println("ids ->" + ids);

            Set<Set<State>> cares = new HashSet<>();

            cares = map.get(t.getEpsilon())
                    .stream()
                    .filter(p -> !p
                            .stream()
                            .filter(s -> ids.contains(s.getId()))
                            .collect(Collectors.toSet()).isEmpty())
                    .collect(Collectors.toSet());

            cares.addAll(map.get(t.getEpsilon())
                    .stream()
                    .filter(p -> !p
                            .stream().filter(s -> s.getId().equals(t.getEpsilon().getId())).collect(Collectors.toSet())
                            .isEmpty())
                    .collect(Collectors.toSet()));

            gp.addln("\t" + t.getEpsilon().getId() + "-> decides for:\n");
            gp.addln("\t\t Itself: " + t.getEpsilon() + " \n");
            gp.addln("\t\t Its successors: " + " \n");
            gp.addln("\t\t\t"
                    + temp
                    + " ,\n");
            gp.addln("\t\t Equivalence classes of Intereset for " + t.getEpsilon().getId() + " ->\n");
            gp.addln("\t\t\t"
                    + cares
                    + " ,\n");
        }
        Set<String> qSet = new HashSet<>();

        for (State state : this.ts.getStates()) {
            qSet.addAll(state.getqState());
            gp.addln("\t\t Quotient states for " + state.getId() + " ->\n");
            gp.addln("\t\t\t"
                    + state.getId() + " -:- " + state.getqState()
                    + " ,\n");

            gp.addln("\t\t\t Pre ->"
                    + state.getPre() + " -:- " + state.getId()
                    + " ,\n");

            gp.addln("\t\t\t Post ->"
                    + state.getPost() + " -:- " + state.getId()
                    + " ,\n");
        }
        gp.printText();

    }
}
