package com.syntm.lts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.*;
import org.graphstream.graph.*;
import org.graphstream.graph.Node;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceFactory;

import org.graphstream.graph.implementations.DefaultGraph;
import java.util.function.BiFunction;

import com.google.common.collect.Sets;
import com.syntm.util.Printer;
import com.syntm.util.StringUtil;

public class TS {
    private String name;
    private Set<State> states;
    private State initState;
    private Int Interface;
    private Set<Trans> transitions;
    private BiFunction<State, Listen, Listen> LS;
    private BiFunction<State, Label, Label> L;
    private Set<TS> agents;
    private Set<TS> parameters;
    private String status;
    private Set<String> channels;

    public TS(String name, Set<State> states, State initState, Int interface1, Set<Trans> transitions) {
        this.name = name;
        this.states = states;
        this.initState = initState;
        Interface = interface1;
        this.transitions = transitions;
        this.agents = new HashSet<TS>();
        this.parameters = new HashSet<TS>();
        LS = (s, ls) -> s.setListen(ls);
        this.status = "";
        L = (s, l) -> s.setLabel(l);
        this.channels = new HashSet<>();
        this.channels.addAll(interface1.getChannels());

    }

    public TS(String name) {
        this.name = name;
        this.states = new HashSet<State>();
        this.Interface = new Int(new HashSet<>(), new HashSet<>());
        this.initState = new State();
        this.transitions = new HashSet<Trans>();
        this.agents = new HashSet<TS>();
        this.parameters = new HashSet<TS>();
        LS = (s, ls) -> s.setListen(ls);
        L = (s, l) -> s.setLabel(l);
        this.status = "";
        this.channels = new HashSet<>();

    }

    public void setInterface(Int interface1) {
        Interface = interface1;
        this.channels.addAll(this.Interface.getChannels());
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public Set<State> getStates() {
        return states;
    }

    public State getStateById(String id) {
        for (State st : this.states) {
            if (st.getId().equals(id)) {
                return st;
            }
        }

        return null;
    }

    public void applyLabel(String id, Label l) {
        for (State st : this.states) {
            if (st.getId().equals(id)) {
                this.L.apply(st, l);
            }
        }
    }

    public void setStates(Set<State> states) {
        this.states = states;
    }

    public State getInitState() {
        return initState;
    }

    public void setInitState(String id) {
        this.initState = getStateById(id);
    }

    public Int getInterface() {
        return Interface;
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

    public void toDot() {
        System.out.println("# of states of " + this.name + "-> " + this.getStates().size());
        Printer gp = new Printer(name);
        gp.addln("\ngraph [fontcolor=\"green\",fontsize=14,rankdir=LR,ranksep=0.6,nodesep=0.5" + ",label=\""
                + "\n" + this.getName() + " : CH=" + this.getInterface().getChannels() + ", OUT="
                + this.getInterface().getOutput() + " \"];\n");
        // gp.addln("\nsubgraph cluster_L { \"\" [shape=box fontsize=16 style=\"filled\"
        // label=\n");
        // gp.addln("\"" + this.getInterface().toString());
        // gp.addln(
        // "\nAny generated TS is open to interaction\\l with the external world. Some
        // transtions\\l are only reactions and cannot execute\\l without an external
        // initiator.\\l\\l According to the semantics, a transition is\\l a reaction if
        // its channel is not included in\\l channel labelling of the reached
        // state.\\l\"]}");
        // gp.addln("\n\n\n\n");
        gp.addln("node[shape=circle, style=filled, fixedsize=true, fontsize=10];\n");

        gp.addln("init [shape=point,style=invis];");
        for (State state : this.states) {
            gp.addln("\t" + state.getId().toString() + "[label=\"" + formatListen(state.getListen().getChannels())
                    + "\n\n" + this.shortString(state.getLabel().getChannel()) + "/"
                    + this.shortString(state.getLabel().getOutput()) + "\n\n" + state.getId() + "\"];" + "\n");
        }

        gp.addln("\t" + " init -> " + this.getInitState().getId().toString() + "[penwidth=0,tooltip=\"initial state\"]"
                + ";\n");
        for (Trans t : this.getTransitions()) {
            String source = t.getSource().getId().toString();
            String dest = t.getDestination().getId().toString();
            String action = t.getAction().toString();
            gp.addln("\t" + source + " -> " + dest + "[label=\"" + action + "\"]" + ";\n");
        }

        gp.print();

    }

    public Printer toDot(State simState, Trans transition) {
        Printer gp = new Printer(name);
        gp.addln("\ngraph [fontcolor=\"green\",fontsize=14,rankdir=LR,ranksep=0.6,nodesep=0.5" + ",label=\"" + "\n"
                + this.getName() + " : CH=" + this.getInterface().getChannels() + ", OUT="
                + this.getInterface().getOutput() + " \"];\n");

        gp.addln("node[shape=circle, style=filled, fixedsize=true, fontsize=10];\n");

        gp.addln("init [shape=point,style=invis];");
        for (State state : this.states) {
            if (state.equals(transition.getSource()) || state.equals(transition.getDestination())) {
                gp.addln("\t" + this.formatTSName() + state.getId().toString() + "[label=\""
                        + formatListen(state.getListen().getChannels())
                        + "\n\n" + this.shortString(state.getLabel().getChannel()) + "/"
                        + this.shortString(state.getLabel().getOutput()) + "\n\n" + state.getId() + "\" color=\""
                        + "#f25959\"];" + "\n");
            } else {
                if (state.equals(this.getStateById(simState.getId()))) {
                    gp.addln("\t" + this.formatTSName() + state.getId().toString() + "[label=\""
                            + formatListen(state.getListen().getChannels())
                            + "\n\n" + this.shortString(state.getLabel().getChannel()) + "/"
                            + this.shortString(state.getLabel().getOutput()) + "\n\n" + state.getId() + "\" color=\""
                            + "#f25959\"];" + "\n");

                } else {
                    gp.addln("\t" + this.formatTSName() + state.getId().toString() + "[label=\""
                            + formatListen(state.getListen().getChannels())
                            + "\n\n" + this.shortString(state.getLabel().getChannel()) + "/"
                            + this.shortString(state.getLabel().getOutput()) + "\n\n" + state.getId() + "\"];" + "\n");
                }
            }
        }

        gp.addln("\t" + " init -> " + this.formatTSName() + this.getInitState().getId().toString()
                + "[penwidth=0,tooltip=\"initial state\"]"
                + ";\n");
        for (Trans t : this.getTransitions()) {
            String source = this.formatTSName() + t.getSource().getId().toString();
            String dest = this.formatTSName() + t.getDestination().getId().toString();
            String action = t.getAction().toString();
            if (action.equals(transition.getAction())
                    && source.equals(this.formatTSName() + transition.getSource().getId())
                    && dest.equals(this.formatTSName() + transition.getDestination().getId())) {
                gp.addln("\t" + source + " -> " + dest + "[label=\"" + action + "\" color=\"" + "#f25959\"]" + ";\n");
            } else {
                gp.addln("\t" + source + " -> " + dest + "[label=\"" + action + "\"]" + ";\n");
            }
        }

        return gp.clusterIt(this.formatTSName());
    }

    public String formatTSName() {

        return this.getName().substring(2, this.getName().length() - 2);
    }

    public Printer next(Trans transition) {
        return toDot(new State(), transition);
    }

    public Set<Trans> getTransitions() {
        return transitions;
    }

    public Set<TS> getAgents() {
        return agents;
    }

    public State getStateByComposite(TS t, Set<State> comp) {
        for (State s : t.getStates()) {
            if (s.getComStates().equals(comp)) {
                return s;
            }
        }
        return null;
    }

    public TS getAgentById(String id) {
        TS t = new TS("");
        for (TS ts : agents) {
            if (ts.getName().equals(id.substring(2))) {
                t = ts;
                break;
            }
        }
        return t;
    }

    public Set<TS> getParameters() {
        return parameters;
    }

    public void setTransitions(Set<Trans> transitions) {
        this.transitions = transitions;
    }

    public BiFunction<State, Listen, Listen> getLS() {
        return LS;
    }

    public void setLS(BiFunction<State, Listen, Listen> lS) {
        LS = lS;
    }

    public BiFunction<State, Label, Label> getL() {
        return L;
    }

    public void setL(BiFunction<State, Label, Label> l) {
        L = l;
    }

    public void parseDot(final String filePath) throws IOException {
        Graph g = new DefaultGraph("g");
        FileSource fs = FileSourceFactory.sourceFor(filePath);

        fs.addSink(g);

        try {
            fs.begin(filePath);
            while (fs.nextEvents()) {
                // Optionally some code here ...
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            fs.end();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fs.removeSink(g);
        }
        Set<String> chan = new HashSet<>(Arrays.asList(g.getAttribute("label").toString()
                .replaceAll("MainTS : CH=\\[([\\s\\S]*)\\], OUT=\\[([\\s\\S]*)\\]", "$1")
                .replaceAll("([\\w-])([,]*)([\\s]*)", "$1$2").replaceAll("\n", "").split(",")));

        Set<String> out = new HashSet<>(Arrays.asList(g.getAttribute("label").toString()
                .replaceAll("MainTS : CH=\\[([\\s\\S]*)\\], OUT=\\[([\\s\\S]*)\\]", "$2")
                .replaceAll("([\\w-])([,]*)([\\s]*)", "$1$2").replaceAll("\n", "").split(",")));
        this.setInterface(new Int(chan, out));

        for (Node node : g) {
            if (!node.getId().equals("init") && !node.getId().equals("spec")) {
                String[] parts = node.getAttribute("label").toString().split("\n\n");
                State st = new State(node.getId());
                Set<String> sch = new HashSet<>(Arrays.asList(parts[0].replaceAll("\\{([\\s\\S]*)\\}", "$1").split(",")));
                Listen ls = new Listen(sch);
                st.setListen(ls);
                Set<String> chs = new HashSet<String>(
                        Arrays.asList(parts[1].replaceAll("([\\w]*)\\/([\\S]*)", "$1").split(",")));
                Set<String> o = new HashSet<>(Arrays.asList(parts[1].replaceAll("([\\w]*)\\/([\\S]*)", "$2").split(",")));
                

                Label l = new Label(chs, o);
                st.setLabel(l);
                this.addState(st);

            }
        }

        g.edges().forEach(e -> {
            if (!e.getSourceNode().getId().equals("init")) {
                this.getTransitions()
                        .add(new Trans(this.getStateById(e.getSourceNode().getId()), e.getAttribute("label").toString(),
                                this.getStateById(e.getTargetNode().getId())));
                this.channels.add(e.getAttribute("label").toString());
                this.getStateById(e.getSourceNode().getId()).getTrans()
                        .add(new Trans(this.getStateById(e.getSourceNode().getId()), e.getAttribute("label").toString(),
                                this.getStateById(e.getTargetNode().getId())));
            } else {
                this.setInitState(e.getTargetNode().getId());
            }
            
        });
        String[] parts = g.getNode("spec").getAttribute("label").toString().split("\n\n");
        for (int i = 1; i < parts.length; i++) {
        Set<String> achs = new HashSet<String>(Arrays.asList(parts[i].replaceAll("([A-Za-z1-9]+): CH=\\[([\\s\\S]*)\\], OUT=\\[([\\s\\S]*)\\]", "$2").split(",")));
        Set<String> ao = new HashSet<>(Arrays.asList(parts[i].replaceAll("([A-Za-z1-9]+): CH=\\[([\\s\\S]*)\\], OUT=\\[([\\s\\S]*)\\]", "$3").split(",")));
        this.initialDecomposition(parts[i].replaceAll("([A-Za-z1-9]+): CH=\\[([\\s\\S]*)\\], OUT=\\[([\\s\\S]*)\\]", "$1"), achs, ao);
        }
            
        

    }

    public void parse(final String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        String line;
        while (!(line = reader.readLine()).equalsIgnoreCase("!")) {
            String[] parts = line.split(":");
            switch (parts[0].trim()) {
                case "Init":
                    this.setInitState(parts[1].trim());
                    break;
                case "Int":
                    Set<String> chan = new HashSet<>(Arrays.asList(parts[1].trim().split(",")));
                    Set<String> out = new HashSet<>(Arrays.asList(parts[2].trim().split(",")));
                    this.setInterface(new Int(chan, out));
                    break;
                case "LS":
                    State st = new State(parts[1].trim());
                    Set<String> sch = new HashSet<>(Arrays.asList(parts[2].trim().split(",")));
                    Listen ls = new Listen(sch);
                    if (this.getStateById(parts[1].trim()) != null) {
                        this.getLS().apply(this.getStateById(parts[1].trim()), ls);
                    } else {
                        this.addState(st);
                        this.getLS().apply(st, ls);
                    }
                    break;
                case "L":
                    Set<String> chs = new HashSet<String>(Arrays.asList(parts[2].trim().split(",")));
                    Set<String> o = new HashSet<>(Arrays.asList(parts[3].trim().split(",")));
                    Label l = new Label(chs, o);
                    this.applyLabel(parts[1].trim(), l);

                    break;
                case "T":
                    String[] t = parts[1].trim().split(",");

                    this.getTransitions()
                            .add(new Trans(this.getStateById(t[0].trim()), t[1].trim(),
                                    this.getStateById(t[2].trim())));
                    this.channels.add(t[1].trim());
                    this.getStateById(t[0].trim()).getTrans()
                            .add(new Trans(this.getStateById(t[0].trim()), t[1].trim(),
                                    this.getStateById(t[2].trim())));
                    break;
                case "A":
                    Set<String> achs = new HashSet<String>(Arrays.asList(parts[2].trim().split(",")));
                    Set<String> ao = new HashSet<>(Arrays.asList(parts[3].trim().split(",")));
                    this.initialDecomposition(parts[1].trim(), achs, ao);
                    break;
                default:
                    break;
            }

        }

        reader.close();

    }

    public State getStateByComposiStates(TS t, State s_1, State s_2) {
        for (State s : t.getStates()) {
            if (s.getComStates().equals(new HashSet<>(Arrays.asList(s_1, s_2)))) {
                return s;
            }
        }
        return null;
    }

    public TS openParallelCompTS(TS T2) {

        TS t = new TS(this.getName() + " || " + T2.getName());
        if (this.getName().equals("")) {
            t.setName(T2.getName());
        }
        if (T2.getName().equals("")) {
            t.setName(this.getName());
        }

        Set<String> chan = new HashSet<>(this.getInterface().getChannels());
        Set<String> output = new HashSet<>(this.getInterface().getOutput());

        chan.addAll(T2.getInterface().getChannels());
        output.addAll(T2.getInterface().getOutput());

        Int i = new Int(chan, output);
        t.setInterface(i);
        for (State s_1 : this.getStates()) {
            for (State s_2 : T2.getStates()) {
                State sc = new State(s_1.getId() + "" + s_2.getId());
                sc.setComStates(new HashSet<>(Arrays.asList(s_1, s_2)));
                Set<String> ls = new HashSet<>(s_1.getListen().getChannels());
                ls.addAll(s_2.getListen().getChannels());
                Set<String> ch = new HashSet<>(s_1.getLabel().getChannel());
                ch.addAll(s_2.getLabel().getChannel());
                Set<String> out = new HashSet<>(s_1.getLabel().getOutput());
                out.addAll(s_2.getLabel().getOutput());
                if (out.size() > 1) {
                    out.remove("-");
                }
                sc.setListen(new Listen(ls));
                sc.setLabel(new Label(ch, out));
                t.addState(sc);
                if (sc.getComStates().contains(s_1.getOwner().getInitState())
                        && sc.getComStates().contains(s_2.getOwner().getInitState())) {
                    t.setInitState(sc.getId());

                }

            }
        }

        for (State sc : t.getStates()) {
            Set<Trans> trs_1 = new HashSet<>();
            Set<Trans> trs_2 = new HashSet<>();
            int n = sc.getComStates().size();
            State cStates[] = new State[n];
            cStates = sc.getComStates().toArray(cStates);

            trs_1.addAll(cStates[0].getTrans());
            trs_2.addAll(cStates[1].getTrans());

            trs_1.addAll(cStates[0].getTrans());
            trs_2.addAll(cStates[1].getTrans());

            if (trs_2.isEmpty()) {
                for (Trans t1 : trs_1) {
                    t.addTransition(t, t.getStateByComposiStates(t, t1.getSource(), cStates[1]),
                            t1.getAction(),
                            t.getStateByComposiStates(t, t1.getDestination(), cStates[1]));

                    t.getStateByComposiStates(t, t1.getSource(), cStates[1])
                            .addTrans(new Trans(t.getStateByComposiStates(t, t1.getSource(), cStates[1]),
                                    t1.getAction(),
                                    t.getStateByComposiStates(t, t1.getDestination(), cStates[1])), t);
                }
            }
            if (trs_1.isEmpty()) {
                for (Trans t2 : trs_2) {
                    t.addTransition(t, t.getStateByComposiStates(t, t2.getSource(), cStates[0]),
                            t2.getAction(),
                            t.getStateByComposiStates(t, t2.getDestination(), cStates[0]));

                    t.getStateByComposiStates(t, t2.getSource(), cStates[0])
                            .addTrans(new Trans(t.getStateByComposiStates(t, t2.getSource(), cStates[0]),
                                    t2.getAction(),
                                    t.getStateByComposiStates(t, t2.getDestination(), cStates[0])), t);
                }
            } else {

                for (Trans t1 : trs_1) {

                    for (Trans t2 : trs_2) {
                        if (t1.getAction().equals(t2.getAction())
                                && !t1.getSource().getOwner().getInterface().getChannels().contains(t1.getAction())
                                && !t2
                                        .getSource().getOwner().getInterface().getChannels()
                                        .contains(t1.getAction())) {

                            t.addTransition(t, t.getStateByComposiStates(t, t1.getSource(), t2.getSource()),
                                    t1.getAction(),
                                    t.getStateByComposiStates(t, t1.getDestination(), t2.getDestination()));

                            t.getStateByComposiStates(t, t1.getSource(), t2.getSource())
                                    .addTrans(new Trans(t.getStateByComposiStates(t, t1.getSource(), t2.getSource()),
                                            t1.getAction(),
                                            t.getStateByComposiStates(t, t1.getDestination(), t2.getDestination())), t);

                        }
                        if (t1.getAction().equals(t2.getAction())
                                && (t1.getSource().getOwner().getInterface().getChannels().contains(t1.getAction())
                                        || t2
                                                .getSource().getOwner().getInterface().getChannels()
                                                .contains(t1.getAction()))) {

                            t.addTransition(t, t.getStateByComposiStates(t, t1.getSource(), t2.getSource()),
                                    t1.getAction(),
                                    t.getStateByComposiStates(t, t1.getDestination(), t2.getDestination()));

                            t.getStateByComposiStates(t, t1.getSource(), t2.getSource())
                                    .addTrans(new Trans(t.getStateByComposiStates(t, t1.getSource(), t2.getSource()),
                                            t1.getAction(),
                                            t.getStateByComposiStates(t, t1.getDestination(), t2.getDestination())), t);

                        } else {
                            if (!t1.getSource().getOwner().getInterface().getChannels().contains(t1.getAction())
                                    && !t2.getSource().getOwner().getInterface().getChannels()
                                            .contains(t1.getAction())
                                    && !t2.getSource().getListen().getChannels().contains(t1.getAction())) {
                                t.addTransition(t, t.getStateByComposiStates(t, t1.getSource(), t2.getSource()),
                                        t1.getAction(),
                                        t.getStateByComposiStates(t, t1.getDestination(), t2.getSource()));

                                t.getStateByComposiStates(t, t1.getSource(), t2.getSource())
                                        .addTrans(new Trans(
                                                t.getStateByComposiStates(t, t1.getSource(), t2.getSource()),
                                                t1.getAction(),
                                                t.getStateByComposiStates(t, t1.getDestination(), t2.getSource())), t);

                            }

                            if (!t1.getSource().getOwner().getInterface().getChannels().contains(t2.getAction())
                                    && !t2.getSource().getOwner().getInterface().getChannels()
                                            .contains(t2.getAction())
                                    && !t1.getSource().getListen().getChannels().contains(t2.getAction())) {
                                t.addTransition(t, t.getStateByComposiStates(t, t1.getSource(), t2.getSource()),
                                        t2.getAction(),
                                        t.getStateByComposiStates(t, t1.getDestination(), t2.getDestination()));

                                t.getStateByComposiStates(t, t1.getSource(), t2.getSource())
                                        .addTrans(new Trans(
                                                t.getStateByComposiStates(t, t1.getSource(), t2.getSource()),
                                                t1.getAction(),
                                                t.getStateByComposiStates(t, t1.getSource(), t2.getDestination())), t);

                            }
                            if (!t2.getSource().getListen().getChannels().contains(t1.getAction())) {
                                t.addTransition(t, t.getStateByComposiStates(t, t1.getSource(), t2.getSource()),
                                        t1.getAction(),
                                        t.getStateByComposiStates(t, t1.getDestination(), t2.getSource()));

                                t.getStateByComposiStates(t, t1.getSource(), t2.getSource())
                                        .addTrans(new Trans(
                                                t.getStateByComposiStates(t, t1.getSource(), t2.getSource()),
                                                t1.getAction(),
                                                t.getStateByComposiStates(t, t1.getDestination(), t2.getSource())), t);

                            }
                            if (!t1.getSource().getListen().getChannels().contains(t2.getAction())) {
                                t.addTransition(t, t.getStateByComposiStates(t, t1.getSource(), t2.getSource()),
                                        t2.getAction(),
                                        t.getStateByComposiStates(t, t1.getSource(), t2.getDestination()));

                                t.getStateByComposiStates(t, t1.getSource(), t2.getSource())
                                        .addTrans(new Trans(
                                                t.getStateByComposiStates(t, t1.getSource(), t2.getSource()),
                                                t2.getAction(),
                                                t.getStateByComposiStates(t, t1.getSource(), t2.getDestination())), t);
                            }
                            if (!t2.getSource().getListen().getChannels().contains(t1.getAction())
                                    && t1.getSource().getOwner().getInterface().getChannels()
                                            .contains(t1.getAction())) {
                                t.addTransition(t, t.getStateByComposiStates(t, t1.getSource(), t2.getSource()),
                                        t1.getAction(),
                                        t.getStateByComposiStates(t, t1.getDestination(), t2.getSource()));

                                t.getStateByComposiStates(t, t1.getSource(), t2.getSource())
                                        .addTrans(new Trans(
                                                t.getStateByComposiStates(t, t1.getSource(), t2.getSource()),
                                                t1.getAction(),
                                                t.getStateByComposiStates(t, t1.getDestination(), t2.getSource())), t);

                            }
                            if (!t1.getSource().getListen().getChannels().contains(t2.getAction())
                                    && (t2.getSource().getOwner().getInterface().getChannels()
                                            .contains(t2.getAction()))) {
                                t.addTransition(t, t.getStateByComposiStates(t, t1.getSource(), t2.getSource()),
                                        t2.getAction(),
                                        t.getStateByComposiStates(t, t1.getSource(), t2.getDestination()));

                                t.getStateByComposiStates(t, t1.getSource(), t2.getSource())
                                        .addTrans(new Trans(
                                                t.getStateByComposiStates(t, t1.getSource(), t2.getSource()),
                                                t2.getAction(),
                                                t.getStateByComposiStates(t, t1.getSource(), t2.getDestination())), t);
                            }
                        }
                    }

                }
            }

        }

        t.setStates(reachFrom(t, t.getInitState()));
        Set<Trans> trans = new HashSet<>();
        for (Trans tr : t.getTransitions()) {
            if (!t.getStates().contains(tr.getSource())) {
                trans.add(tr);
            }
        }
        t.transitions.removeAll(trans);
        return t;
    }

    public Set<State> reachFrom(TS ts, State s) {

        HashMap<State, Boolean> visited = new HashMap<>();
        for (State st : ts.getStates()) {
            visited.put(st, false);
        }
        LinkedList<State> queue = new LinkedList<State>();
        Set<Trans> transitiveC = new HashSet<>();
        Set<State> reach = new HashSet<>();
        visited.put(s, false);
        queue.add(s);
        while (queue.size() != 0) {
            s = queue.poll();
            reach.add(s);
            transitiveC = hasTransitions(ts, s);
            if (transitiveC != null) {
                if (transitiveC.size() != 0) {
                    for (Trans tr : transitiveC) {
                        if (!visited.get(tr.getDestination())) {
                            visited.put(tr.getDestination(), true);
                            queue.add(tr.getDestination());
                        }
                    }
                }
            }
        }
        return reach;
    }

    public void initialDecomposition(String name, Set<String> channels, Set<String> outputs) {
        TS t = new TS(name);
        TS p = new TS("P-" + name);
        Int i = new Int(channels, outputs);
        Set<String> mCh = new HashSet<>();
        Set<String> mOut = new HashSet<>();
        Set<String> pCh = new HashSet<>(this.getInterface().getChannels());
        pCh.removeAll(channels);

        Set<String> pOut = new HashSet<>(this.getInterface().getOutput());
        pOut.removeAll(outputs);
        mCh.addAll(pCh);
        mCh.addAll(channels);
        mCh.remove("");
        mOut.addAll(outputs);
        mOut.addAll(pOut);
        Int pInt = new Int(pCh, pOut);
        this.setInterface(new Int(mCh, mOut));
        t.setInterface(i);
        p.setInterface(pInt);

        for (State s : this.getStates()) {
            Set<String> chan = new HashSet<>(i.getChannels());
            Set<String> pChan = new HashSet<>(pInt.getChannels());
            Set<String> mChan = new HashSet<>();
            Set<String> mO = new HashSet<>();
            chan.retainAll(s.getLabel().getChannel());
            pChan.retainAll(s.getLabel().getChannel());
            Set<String> out = new HashSet<>(i.getOutput());
            Set<String> pO = new HashSet<>(pInt.getOutput());
            out.retainAll(s.getLabel().getOutput());
            if (out.isEmpty()) {
                out.add("-");
            }
            pO.retainAll(s.getLabel().getOutput());
            if (pO.isEmpty()) {
                pO.add("-");
            }
            mChan.addAll(chan);
            mChan.addAll(pChan);
            mO.addAll(out);
            mO.addAll(pO);
            if (mO.size() > 1) {
                mO.remove("-");
            }
            s.setLabel(new Label(mChan, mO));
            Label lab = new Label(chan, out);
            Label pLab = new Label(pChan, pO);
            State st = new State(s.getId(), lab, s.getListen());
            State pSt = new State(s.getId(), pLab, s.getListen());
            t.addState(st);
            p.addState(pSt);

        }

        t.setInitState(this.getInitState().getId());
        p.setInitState(this.getInitState().getId());
        for (Trans tr : this.getTransitions()) {
            t.addTransition(t, t.getStateById(tr.source.getId()), tr.action, t.getStateById(tr.destination.getId()));

            p.addTransition(p, p.getStateById(tr.source.getId()), tr.action, p.getStateById(tr.destination.getId()));

            t.getStateById(tr.source.getId()).getTrans().add(
                    new Trans(t.getStateById(tr.source.getId()), tr.action, t.getStateById(tr.destination.getId())));
            t.getStateById(tr.source.getId()).getPost().add(t.getStateById(tr.destination.getId()));

            t.getStateById(tr.destination.getId()).getPre().add(t.getStateById(tr.source.getId()));

            p.getStateById(tr.source.getId()).getTrans().add(
                    new Trans(p.getStateById(tr.source.getId()), tr.action, p.getStateById(tr.destination.getId())));
            p.getStateById(tr.source.getId()).getPost().add(p.getStateById(tr.destination.getId()));
            p.getStateById(tr.destination.getId()).getPre().add(t.getStateById(tr.source.getId()));
        }
        for (State pr : this.getStates()) {
            pr.getTrans().clear();
        }
        for (Trans tr : this.getTransitions()) {
            for (State state : this.getStates()) {
                if (tr.getSource().getId().equals(state.getId())) {
                    tr.setSource(state);
                    tr.setDestination(this.getStateById(tr.getDestination().getId()));
                    state.addTrans(tr, state.getOwner());
                }
            }
        }

        this.agents.add(t.reduce());
        this.parameters.add(p);
        // t.toDot();
        // p.toDot();
    }

    public Set<List<State>> cartesianProduct(List<Set<State>> sets) {
        Set<List<State>> cartesianProduct = Sets.cartesianProduct(sets);
        return cartesianProduct;
    }

    public TS close() {
        Set<Trans> remove = this.getInitState().getTrans().stream()
                .filter(tr -> !tr.getDestination().getLabel().getChannel().contains(tr.getAction()))
                .collect(Collectors.toSet());

        this.getInitState().getTrans().removeAll(remove);
        this.getTransitions().removeAll(remove);
        Set<Trans> last = this.getTransitions().stream()
                .filter(tran -> !tran.getDestination().getLabel().getChannel().contains(tran.getAction()))
                .collect(Collectors.toSet());
        for (Trans trans : last) {

            this.getStateById(trans.getSource().getId()).getTrans().remove(trans);

        }
        this.getTransitions().removeAll(last);
        this.setStates(reachFrom(this, this.getInitState()));
        Set<Trans> trans = new HashSet<>();
        for (Trans tr : this.getTransitions()) {
            if (!this.getStates().contains(tr.getSource())) {
                trans.add(tr);
            }
        }

        this.getTransitions().removeAll(trans);

        return this;
    }

    public void addTransition(TS ts, State src, String action, State des) {
        src.setOwner(ts);
        des.setOwner(ts);
        ts.transitions.add(new Trans(src, action, des));
        this.channels.add(action);

    }

    public void addState(State state) {
        state.setOwner(this);
        this.states.add(state);
    }

    public void setName(String name) {
        this.name = name;
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
        TS other = (TS) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
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
        if (L == null) {
            if (other.L != null)
                return false;
        } else if (!L.equals(other.L))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append("\n\n");
        b.append(this.name + " [ \n\n");

        b.append("States = (");
        b.append(StringUtil.join(this.getStates(), ", ") + " ),\n\n");
        b.append("Initial State = ");
        b.append(StringUtil.join(new HashSet<State>(Arrays.asList(this.getInitState())), ",") + ",\n\n");
        b.append("Interface = ");
        b.append(StringUtil.join(new HashSet<Int>(Arrays.asList(this.getInterface())), ",") + ",\n\n");

        b.append("Transitions = ( ");
        b.append(StringUtil.join(this.getTransitions(), ", ") + " )\n\n");
        b.append("Agents = ( ");
        b.append(StringUtil.join(this.getAgents(), ", ") + " )\n\n");
        b.append("]\n\n");
        b.append("Parameters = ( ");
        b.append(StringUtil.join(this.parameters, ", ") + " )\n\n");
        b.append("]\n\n");
        return b.toString();
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void setAgents(Set<TS> agents) {
        this.agents = agents;
    }

    public void setParameters(Set<TS> parameters) {
        this.parameters = parameters;
    }

    public void setChannels(Set<String> channels) {
        this.channels = channels;
    }

    public Set<String> getChannels() {
        return channels;
    }

    public Boolean equivCheck(TS ts) {
        Set<Set<State>> rho = this.rhoCompute(ts);
        // System.err.println(rho);
        Set<State> res = this.popStates(rho
                .stream()
                .filter(p -> p.contains(this.getInitState()))
                .collect(Collectors.toSet()));

        if (res.contains(ts.getInitState())) {
            return true;
        }
        return false;
    }

    public Set<Set<State>> rhoCompute(TS ts) {

        Set<Set<State>> rhoinit = new HashSet<>();
        Set<State> stateSet = new HashSet<>();
        if (!ts.getStates().isEmpty()) {
            stateSet.addAll(ts.getStates());
        }
        stateSet.addAll(this.getStates());
        rhoinit.add(stateSet);
        Set<String> sigma = new HashSet<String>();
        sigma.addAll(this.channels);
        if (!ts.getChannels().isEmpty()) {
            sigma.addAll(ts.getChannels());
        }

        Set<Set<State>> rho = new HashSet<Set<State>>(rhoinit);

        Set<Label> labs = new HashSet<>();
        for (State s : stateSet) {
            labs.add(s.getLabel());
        }
        for (Label l : labs) {
            HashMap<Set<State>, Set<State>> splitters = new HashMap<>();
            for (Set<State> partition : rho) {
                if (partition.size() > 1) {
                    Set<State> splitter = findSplit(partition, l);
                    if (!splitter.isEmpty() && !splitter.equals(partition)) {
                        splitters.put(partition, splitter);
                    }
                }
            }
            if (!splitters.isEmpty()) {
                for (Set<State> p : splitters.keySet()) {
                    Set<Set<State>> splitP = split(p, splitters.get(p));
                    rho.remove(p);
                    rho.addAll(splitP);
                }
            }
        }

        Set<Set<State>> waiting = new HashSet<Set<State>>(rho);

        Boolean changedW = true;
        while (changedW) {
            Set<State> pprime = this.popStates(waiting);
            for (String action : sigma) {
                HashMap<Set<State>, Set<State>> splitters = new HashMap<>();
                for (Set<State> partition : rho) {
                    Set<State> splitter = this.strongBism(partition, pprime, action);

                    if (!splitter.isEmpty() && !splitter.equals(partition)) {
                        splitters.put(partition, splitter);
                    }
                }
                for (Set<State> p : splitters.keySet()) {
                    Set<Set<State>> splitP = split(p, splitters.get(p));
                    rho.remove(p);
                    rho.addAll(splitP);
                    waiting.remove(p);
                    waiting.addAll(splitP);
                }
            }
            if (waiting.isEmpty()) {
                changedW = false;
            }
        }
        return rho;
    }

    public TS reduce() {
        Set<Set<State>> rho = new HashSet<>(this.rhoCompute(new TS("")));
        if (rho.size() == 1) {
            return this;
        }
        CompressedTS c = new CompressedTS("r-" + this.getName());
        TS t = c.DoQuotient(this, rho);
        return t;
    }

    private Set<Trans> hasTransitions(TS ts, State s) {
        Set<Trans> trSet = new HashSet<>();

        for (Trans tr : ts.getTransitions()) {
            try {
                if (tr.getSource().equals(s)) {
                    trSet.add(tr);
                }
            } catch (Exception e) {
                System.out.println(e);
            }

        }

        return trSet;
    }

    private Set<State> popStates(Set<Set<State>> stateSets) {
        Set<State> states = stateSets.iterator().next();
        stateSets.remove(states);
        return states;
    }

    private Set<State> strongBism(Set<State> partition, Set<State> pprime, String action) {
        Set<State> acc = new HashSet<>();
        // acc = partition
        // .stream()
        // .filter(
        // s -> !(s.getTrans()
        // .stream()
        // .filter(tr -> tr.getAction().equals(action) &&
        // pprime.contains(tr.getDestination()))
        // .collect(Collectors.toSet())).isEmpty())
        // .collect(Collectors.toSet());

        for (State s : partition) {
            for (Trans tr : s.getTrans()) {
                if (tr.getAction().equals(action) && pprime.contains(tr.getDestination())) {
                    acc.add(s);
                    break;
                }
            }
        }

        return acc;
    }

    private Set<State> findSplit(Set<State> p, Label label) {
        Set<State> out = new HashSet<State>();
        out = p.stream().filter(s -> s.getLabel().equals(label)).collect(Collectors.toSet());
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
