package com.syntm.lts;
/*
Author:  Yehia Abd Alrahman (yehiaa@chalmers.se)
CompressedTS.java (c) 2024
Desc: Compute Quotient TS/Compress TS
Created:  2024-11-17T10:50:26.221Z
Updated:  17/11/2024 21:56:40
Version:  1.1
*/

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.syntm.util.Printer;

public class CompressedTS {
    private String name;
    private Integer index;
    private Set<PartitionState> states;
    private PartitionState initState;
    private Int Interface;
    private Set<PartitionTrans> PartitionTransitions;
    private BiFunction<PartitionState, Listen, Listen> LS;
    private BiFunction<PartitionState, Label, Label> L;
    Set<Set<State>> rho = new HashSet<Set<State>>();

    public CompressedTS(String name, Set<PartitionState> states, PartitionState initState, Int interface1,
            Set<PartitionTrans> PartitionTransitions) {
        this.name = name;
        this.states = states;
        this.initState = initState;
        Interface = interface1;
        this.PartitionTransitions = PartitionTransitions;
        LS = (s, ls) -> s.setListen(ls);

        L = (s, l) -> s.setLabel(l);
        this.index = 0;

    }

    public CompressedTS(String name) {
        this.name = name;
        this.states = new HashSet<PartitionState>();
        this.Interface = new Int(null, null);
        this.initState = new PartitionState("-1");
        this.PartitionTransitions = new HashSet<PartitionTrans>();
        LS = (s, ls) -> s.setListen(ls);
        L = (s, l) -> s.setLabel(l);
        this.index = 0;
    }

    public CompressedTS(CompressedTS TS) {
        this.name = TS.getName() + "copy";
        this.states = TS.getStates();
        this.Interface = TS.getInterface();
        this.initState = TS.getInitState();
        this.PartitionTransitions = TS.PartitionTransitions;
        LS = TS.getLS();
        L = TS.getL();
        this.index = 0;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public void setInterface(Int interface1) {
        Interface = interface1;
    }

    public String getName() {
        return name;
    }

    public Set<PartitionState> getStates() {
        return states;
    }

    public void applyLabel(String id, Label l) {
        for (PartitionState st : this.states) {
            if (st.getId().equals(id)) {
                this.L.apply(st, l);
            }
        }
    }

    public void setStates(Set<PartitionState> states) {
        this.states = states;
    }

    public PartitionState getInitState() {
        return initState;
    }

    public void addPartitionState(PartitionState partitionState) {
        partitionState.setOwner(this);
        this.states.add(partitionState);

    }

    public void setInitState(String id) {
        this.initState = getStateById(id);
    }

    public PartitionState getStateById(String id) {
        for (PartitionState st : this.states) {
            if (st.getId().equals(id)) {
                return st;
            }
        }

        return null;
    }

    public Int getInterface() {
        return Interface;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<PartitionTrans> getPartitionTransitions() {
        return PartitionTransitions;
    }

    public void setPartitionTransitions(Set<PartitionTrans> PartitionTransitions) {
        this.PartitionTransitions = PartitionTransitions;
    }

    public BiFunction<PartitionState, Listen, Listen> getLS() {
        return LS;
    }

    public void setLS(BiFunction<PartitionState, Listen, Listen> lS) {
        LS = lS;
    }

    public BiFunction<PartitionState, Label, Label> getL() {
        return L;
    }

    public void setL(BiFunction<PartitionState, Label, Label> l) {
        L = l;
    }

    public String shortString(Set<String> longString) {
        String st = "";
        for (String string : longString) {
            st += string + ",";
        }
        if (st.endsWith(",")) {
            st = st.substring(0, st.length() - 1);
        }

        return st;
    }

    public String formatListen(Set<String> longString) {
        String st = "{";
        for (String string : longString) {
            st += string + ",";
        }
        if (st.endsWith(",")) {
            st = st.substring(0, st.length() - 1);
        }
        st += "}";
        return st;
    }

    public void toDot(CompressedTS ts, String name) {

        Printer gp = new Printer(name);
        gp.addln("\ngraph [rankdir=LR,ranksep=.6,nodesep=0.5];\n");
        gp.addln("node[shape=circle style=filled fixedsize=true fontsize=10]\n");

        gp.addln("init [shape=point,style=invis];");
        for (PartitionState state : ts.states) {
            gp.addln("\t" + state.getId().toString() + "[label=\"" + formatListen(state.getListen().getChannels())
                    + "\n\n" + this.shortString(state.getLabel().getChannel()) + "/"
                    + this.shortString(state.getLabel().getOutput()) + "\n\n\n" + "\"]" + "\n");
        }

        gp.addln("\t" + " init -> " + ts.getInitState().getId().toString() + "[penwidth=0,tooltip=\"initial state\"]"
                + ";\n");
        for (PartitionTrans t : ts.getPartitionTransitions()) {
            String source = t.getSource().getId().toString();
            String dest = t.getDestination().getId().toString();
            String action = t.getAction().toString();
            gp.addln("\t" + source + " -> " + dest + "[label=\"" + action + "\"]" + ";\n");
        }

        gp.print();

    }
    public TS DoQuotient(TS ts, Set<Set<State>> rho) {
        //System.out.println("This is rho -> "+rho);
        CompressedTS t = new CompressedTS("");
        
        if (ts.getName().contains("T[")) {
            t.setName(ts.getName());
        } else {
            t.setName(ts.getName());
        }
        Int i = new Int(ts.getInterface().getChannels(), ts.getInterface().getOutput());
        t.setInterface(i);

        for (Set<State> p : rho) {
            PartitionState s_rho = new PartitionState();
            t.getL().apply(s_rho, p.iterator().next().getLabel());
            s_rho.setPartition(p);
            if (p.contains(ts.getInitState())) {
                s_rho.setId(ts.getInitState().getId());
                t.getLS().apply(s_rho, new Listen(ts.getInitState().getListen().getChannels()));
                t.addState(s_rho);
                t.setInitState(s_rho.getId());
            } else {
                s_rho.setId(p.iterator().next().getId());
                t.addState(s_rho);
            }

        }
        for (PartitionState pState : t.getStates()) {
            for (State state : pState.getPartition()) {
                for (Trans tr : state.getTrans()) {
                        t.addPartitionTransition(t, pState, tr.action, t.getPstateByMembership(tr.getDestination()));

                        pState.getPartitionTrans().add(
                                new PartitionTrans(pState, tr.action, t.getPstateByMembership(tr.getDestination())));
                }
            }

        }

        for (PartitionState s : t.getStates()) {
            Set<String> chan = new HashSet<String>();
            for (PartitionTrans tr : s.getPartitionTrans()) {
                chan.add(tr.getAction());
            }
            Listen ls = new Listen(chan);
            t.getLS().apply(s, ls);
        }
        TS convT = t.convetToTS(t,ts);
       // convT.toDot();
        return convT;
    }
    public TS DoCompress(TS ts, Set<Set<State>> rho) {
        CompressedTS t = new CompressedTS("");
        if (ts.getName().contains("T[")) {
            t.setName(ts.getName());
        } else {
            t.setName("[" + ts.getName() + "]");
        }
        Int i = new Int(ts.getInterface().getChannels(), ts.getInterface().getOutput());
        t.setInterface(i);

        for (Set<State> p : rho) {
            PartitionState s_rho = new PartitionState();
            t.getL().apply(s_rho, p.iterator().next().getLabel());
            s_rho.setPartition(p);
            if (p.contains(ts.getInitState())) {
                s_rho.setId(ts.getInitState().getId());
                t.getLS().apply(s_rho, new Listen(ts.getInitState().getListen().getChannels()));
                t.addState(s_rho);
                t.setInitState(s_rho.getId());
            } else {
                s_rho.setId(p.iterator().next().getId());
                t.addState(s_rho);
            }

        }
        for (PartitionState pState : t.getStates()) {
            for (State state : pState.getPartition()) {
                for (Trans tr : state.getTrans()) {
                    if (t.getInterface().getChannels().contains(tr.action)) {
                        t.addPartitionTransition(t, pState, tr.action, t.getPstateByMembership(tr.getDestination()));

                        pState.getPartitionTrans().add(
                                new PartitionTrans(pState, tr.action, t.getPstateByMembership(tr.getDestination())));
                    }
                    if (!t.getInterface().getChannels().contains(tr.action)
                            && !pState.getPartition().contains(tr.getDestination())) {
                        t.addPartitionTransition(t, pState, tr.action, t.getPstateByMembership(tr.getDestination()));
                        pState.getPartitionTrans().add(
                                new PartitionTrans(pState, tr.action, t.getPstateByMembership(tr.getDestination())));
                    }
                }
            }

        }

        for (PartitionState s : t.getStates()) {
            Set<String> chan = new HashSet<String>();
            for (PartitionTrans tr : s.getPartitionTrans()) {
                chan.add(tr.getAction());
            }
            Listen ls = new Listen(chan);
            t.getLS().apply(s, ls);
        }
        TS convT = t.convetToTS(t,ts);
        //convT.toDot();
        return convT;

    }

    private TS convetToTS(CompressedTS t, TS tss) {
        TS ts = new TS(t.getName());
        ts.setParameters(tss.getParameters());
        ts.setAgents(tss.getAgents());
        ts.setInterface(t.getInterface());
        //ts.setRho(tss.getRho());
        for (PartitionState pState : t.getStates()) {
            State st = new State("");
            st = pState.toState();
            st.setqState(pState.getPartition().stream().map(State::getId).collect(Collectors.toSet()));
            ts.addState(st);
        }
        ts.setInitState(t.getInitState().getId());

        for (PartitionTrans tr : t.getPartitionTransitions()) {

            ts.getTransitions().add(tr.toTrans());
            ts.getStateById(tr.getSource().getId()).addTrans(tr.toTrans(), ts);

            ts.getStateById(tr.getSource().getId()).getPost().add(ts.getStateById(tr.getDestination().getId()));

            ts.getStateById(tr.getDestination().getId()).getPre().add(ts.getStateById(tr.getSource().getId()));
        }

        return ts;

    }

    public void addTransition(CompressedTS ts, PartitionState src, String action, PartitionState des) {
        src.setOwner(ts);
        des.setOwner(ts);
        ts.PartitionTransitions.add(new PartitionTrans(src, action, des));

    }

    public PartitionState getStateByComposiStates(CompressedTS t, PartitionState s_1, PartitionState s_2) {
        for (PartitionState s : t.getStates()) {
            if (s.getComStates().equals(new HashSet<>(Arrays.asList(s_1, s_2)))) {
                return s;
            }
        }
        return null;
    }

    private void addPartitionTransition(CompressedTS t, PartitionState pState, String action,
            PartitionState dState) {
        pState.setOwner(t);
        dState.setOwner(t);
        t.getPartitionTransitions().add(new PartitionTrans(pState, action, dState));

    }

    private PartitionState getPstateByMembership(State state) {
        for (PartitionState pState : this.getStates()) {
            if (pState.getPartition().contains(state)) {
                return pState;
            }
        }
        return null;
    }

    private void addState(PartitionState s_rho) {
        s_rho.setOwner(this);
        this.states.add(s_rho);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((Interface == null) ? 0 : Interface.hashCode());
        result = prime * result + ((L == null) ? 0 : L.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CompressedTS other = (CompressedTS) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (states == null) {
            if (other.states != null)
                return false;
        } else if (!states.equals(other.states))
            return false;
        if (initState == null) {
            if (other.initState != null)
                return false;
        } else if (!initState.equals(other.initState))
            return false;
        if (Interface == null) {
            if (other.Interface != null)
                return false;
        } else if (!Interface.equals(other.Interface))
            return false;
        if (PartitionTransitions == null) {
            if (other.PartitionTransitions != null)
                return false;
        } else if (!PartitionTransitions.equals(other.PartitionTransitions))
            return false;
        if (LS == null) {
            if (other.LS != null)
                return false;
        } else if (!LS.equals(other.LS))
            return false;
        if (L == null) {
            if (other.L != null)
                return false;
        } else if (!L.equals(other.L))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CompressedTS [name=" + name + ", states=" + states + ", initState=" + initState + ", Interface="
                + Interface + ", PartitionTransitions=" + PartitionTransitions + ", LS=" + LS + ", L=" + L + "]";
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
