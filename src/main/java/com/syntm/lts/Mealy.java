package com.syntm.lts;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.syntm.util.GraphPrinter;

public class Mealy {
    private String name;
    private Set<State> states;
    private State initState;
    private Trans initTrans;
    private Int Interface;
    private Set<Trans> transitions;

    public Mealy(String name, Set<State> states, State initState, Int interface1, Set<Trans> transitions) {
        this.name = name;
        this.states = states;
        this.initState = initState;
        Interface = interface1;
        this.transitions = transitions;
    }

    public Mealy(String name) {
        this.name = name;
        this.states = new HashSet<>();
        this.initState = new State("");
        this.Interface = new Int(new HashSet<>(), new HashSet<>());
        this.transitions = new HashSet<>();
        this.initTrans = new Trans();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<State> getStates() {
        return states;
    }

    public void setStates(Set<State> states) {
        this.states = states;
    }

    public State getInitState() {
        return initState;
    }

    public void setInitState(State initState) {
        this.initState = initState;
    }

    public Int getInterface() {
        return Interface;
    }

    public void setInterface(Int interface1) {
        Interface = interface1;
    }

    public Set<Trans> getTransitions() {
        return transitions;
    }

    public void setTransitions(Set<Trans> transitions) {
        this.transitions = transitions;
    }

    public Trans getInitTrans() {
        return initTrans;
    }

    public void setInitTrans(Trans initTrans) {
        this.transitions.add(initTrans);
        this.initTrans = initTrans;
    }

    public State getStateById(String id) {
        for (State st : this.states) {
            if (st.getId().equals(id)) {
                return st;
            }
        }

        return null;
    }

    public String formatAction(String action) {
        String st = "";
        String[] parts = action.split("/");
        for (String string : parts) {
            st += string;
        }
        return st;
    }

    public String formatChannel(Set<String> longString) {
        String st = "";
        for (String string : longString) {
            st += string;
        }
        return st;
    }

    public String IdState(Trans tr) {
        String s = tr.getSource().getId() + tr.getAction().hashCode() + tr.getDestination().getId();
        return s;
    }

    public Mealy kissToMealy(Mealy m, String name) {
        return null;
    }

    public TS toTS(Mealy m, String name) { // COMPLETE THIS
        TS ts = new TS("T[" + name + "]");
        ts.setInterface(m.getInterface());
        for (Trans tr : m.getTransitions()) {
            String[] parts = tr.getAction().split("/");
            State st = new State(IdState(tr),
                    new Label(new HashSet<>(Arrays.asList(parts[0].trim().split(","))),
                            new HashSet<>(Arrays.asList(parts[1].trim().split(",")))));
            ts.addState(st);
        }
        for (Trans tr_1 : m.getTransitions()) {
            for (Trans tr_2 : m.getTransitions()) {
                if (tr_1.getDestination().equals(tr_2.getSource())) {
                    ts.addTransition(ts,
                            ts.getStateById(IdState(tr_1)),
                            formatChannel(ts.getStateById(IdState(tr_2)).getLabel().getChannel()),
                            ts.getStateById(IdState(tr_2)));

                    ts.getStateById(IdState(tr_1))
                            .addTrans(new Trans(ts.getStateById(IdState(tr_1)),
                                    formatChannel(ts.getStateById(IdState(tr_2)).getLabel().getChannel()),
                                    ts.getStateById(IdState(tr_2))), ts);

                }
            }
        }

        for (State st : ts.getStates()) {
            Set<String> chans = new HashSet<>();
            for (Trans tr : st.getTrans()) {
                chans.add(tr.getAction());
            }
            ts.getLS().apply(st, new Listen(chans));
            
        }

        ts.setInitState(IdState(m.getInitTrans()));
        ts.toDot(ts, ts.getName());
        return ts;
    }

    public void toDot(Mealy m, String name) {

        GraphPrinter gp = new GraphPrinter(name);
        gp.addln("\ngraph [rankdir=LR,ranksep=.6,nodesep=0.5];\n");
        gp.addln("\nsubgraph cluster_L { \"\" [shape=box fontsize=16 style=\"filled\" label=\n");
        gp.addln("\"" + m.getInterface().toString());
        gp.addln(
                "\n The generated Mealy Machine\"]}");
        gp.addln("\n\n\n\n");
        gp.addln("node[shape=circle style=filled fixedsize=true fontsize=10]\n");

        gp.addln(m.initTrans.getSource().getId() + " [shape=point,style=invis];");
        for (State state : m.states) {
            gp.addln("\t" + state.getId().toString() + "[label=\"" + state.getId().toString() + "\"]" + "\n");
        }

        gp.addln("\t" + m.initTrans.getSource().getId() + "-> " + m.getInitState().getId().toString() + "[label=\""
                + this.initTrans.getAction() + "\", penwidth=.5,tooltip=\"initial state\"]"
                + ";\n");
        for (Trans t : m.getTransitions()) {
            String source = t.getSource().getId().toString();
            String dest = t.getDestination().getId().toString();
            String action = t.getAction().toString();
            if (!t.equals(m.getInitTrans())) {
                gp.addln("\t" + source + " -> " + dest + "[label=\"" + action + "\"]" + ";\n");
            }

        }

        gp.print();

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((states == null) ? 0 : states.hashCode());
        result = prime * result + ((initState == null) ? 0 : initState.hashCode());
        result = prime * result + ((initTrans == null) ? 0 : initTrans.hashCode());
        result = prime * result + ((Interface == null) ? 0 : Interface.hashCode());
        result = prime * result + ((transitions == null) ? 0 : transitions.hashCode());
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
        Mealy other = (Mealy) obj;
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
        if (initTrans == null) {
            if (other.initTrans != null)
                return false;
        } else if (!initTrans.equals(other.initTrans))
            return false;
        if (Interface == null) {
            if (other.Interface != null)
                return false;
        } else if (!Interface.equals(other.Interface))
            return false;
        if (transitions == null) {
            if (other.transitions != null)
                return false;
        } else if (!transitions.equals(other.transitions))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Mealy [name=" + name + ", states=" + states + ", initState=" + initState + ", initTrans=" + initTrans
                + ", Interface=" + Interface + ", transitions=" + transitions + "]";
    }

    public static void main(final String[] args) throws IOException {
        Mealy m = new Mealy("M");
        m.setInterface(new Int(new HashSet<>(Arrays.asList("y")), new HashSet<>(Arrays.asList("o"))));
        for (int i = 0; i < 4; i++) {
            m.states.add(new State(i + ""));
        }
        m.setInitState(m.getStateById("0"));
        int i = 0;
        for (State st : m.getStates()) {
            for (State s : m.getStates()) {

                m.transitions.add(new Trans(st, i + "/o", s));
                i += 1;
            }
        }
        m.setInitTrans(new Trans(new State("init"), "0/0", m.getInitState()));
        // m.setInitState(m.getStateById("0"));
        m.toTS(m, m.getName());
        m.toDot(m, m.getName());

    }

}
