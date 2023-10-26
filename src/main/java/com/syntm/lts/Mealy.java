package com.syntm.lts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.lang.Math;
import com.syntm.util.Printer;

public class Mealy {
    private String name;
    private Set<State> states;
    private State initState;
    private Trans initTrans;
    private Int Interface;
    private Set<Trans> transitions;
    public HashMap<String, Integer> getCode() {
        return code;
    }

    public void setCode(HashMap<String, Integer> code) {
        this.code = code;
    }

    private HashMap<String,Integer> code;

    private String status;

    public Mealy(String name, Set<State> states, State initState, Int interface1, Set<Trans> transitions) {
        this.name = name;
        this.states = states;
        this.initState = initState;
        Interface = interface1;
        this.transitions = transitions;
        this.status = "";
        this.code=new HashMap<>();
    }

    public Mealy(String name) {
        this.name = name;
        this.states = new HashSet<>();
        this.initState = new State("");
        this.Interface = new Int(new HashSet<>(), new HashSet<>());
        this.transitions = new HashSet<>();
        this.initTrans = new Trans();
        this.status = "";
        this.code=new HashMap<>();
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
        String s = tr.getSource().getId() + Math.abs(tr.getAction().hashCode()) + tr.getDestination().getId();
        return s;
    }

    public String CodeResolve(String alphabet, String code) {
        String[] alpha = alphabet.split(",");
        String s = "";
        for (int i = 0; i < code.length(); i++) {
            if (code.substring(i, i + 1).equals("1")) {
                s += alpha[i];
            }
        }
        return s;
    }

    public List<String> CodeWildCard(String act, String cCode, String oCode) {
        String[] Alphal = act.split(":");
        List<String> olst = new ArrayList<>();
        List<String> inlst = new ArrayList<>();
        // List<String> dash = new ArrayList<>();
        if (Alphal[0].contains("+")) {
            Alphal[0] = Alphal[0].replace("+", ",");
            String[] s = Alphal[0].split(",");
            for (int i = 0; i < s.length; i++) {
                if (s[i].contains("-")) {
                    inlst = Generate(s[i], cCode);
                }
            }

            for (String c : s) {
                if (!c.contains("-")) {
                    inlst.add(CodeResolve(cCode, c));
                }

            }
        } else {
            if (Alphal[0].contains("-")) {
                inlst = Generate(Alphal[0], cCode);
            } else {
                inlst.add(CodeResolve(cCode, Alphal[0]));
            }
        }
        if (Alphal[1].contains("+")) {
            Alphal[1] = Alphal[1].replace("+", ",");
            String[] s = Alphal[1].split(",");
            for (int i = 0; i < s.length; i++) {
                if (s[i].contains("-")) {
                    olst = Generate(s[i], oCode);
                }
            }
            for (String o : s) {
                if (!o.contains("-")) {
                    olst.add(CodeResolve(oCode, o));
                }
            }
        } else {
            if (Alphal[1].contains("-")) {
                olst = Generate(Alphal[1], oCode);
            } else {

                olst.add(CodeResolve(oCode, Alphal[1]));
            }
        }

        List<String> letters = new ArrayList<>();
        for (String il : inlst) {
            for (String ol : olst) {

                letters.add(il + "/" + ol);
            }
        }
        return letters;
    }

    public List<String> Generate(String letter, String code) {
        String[] c = code.split(",");
        String cO = "";
        String cw = "";
        for (int i = 0; i < letter.length(); i++) {
            if (letter.substring(i, i + 1).equals("-")) {
                cw += c[i] + ",";
            }
            if (letter.substring(i, i + 1).equals("1")) {
                cO += c[i];
            }
        }
        String[] set = cw.split(",");
        List<String> ls = PowerSet(set);
        List<String> mlist = new ArrayList<>();
        for (String l : ls) {
            l += cO;
            mlist.add(l);
        }
        return mlist;
    }

    public List<String> PowerSet(String[] set) {
        String s = "";
        List<String> list = new ArrayList<>();
        long pow_set_size = (long) Math.pow(2, set.length);
        int counter, j;

        for (counter = 0; counter < pow_set_size; counter++) {
            for (j = 0; j < set.length; j++) {

                if ((counter & (1 << j)) > 0) {
                    s += set[j];
                }
            }
            list.add(s);
            s = "";
        }
        return list;
    }

    public void kissToMealy(final String filePath, String cCode, String oCode) throws IOException {
        // Mealy m = new Mealy("Strategy");
        Int mInt = new Int();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        String line;
        String recent = "";
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(" ");
            String[] mParts = new String[parts.length - 1];
            for (int i = 1; i < parts.length; i++) {
                mParts[i - 1] = parts[i];
            }
            switch (parts[0].trim()) {
                case "REALIZABLE":

                    System.out.println(
                            "\n\n Specification is REALIZABLE for a single agent\n\n You may get a distributed Implementation!\n\n");
                    this.setStatus("REALIZABLE");
                    this.setName("Strategy");
                    break;
                case "UNREALIZABLE":
                    this.setName("Counter Strategy");
                    System.out.println("\n\n Specification is UNREALIZABLE\n\n You will get a counter strategy :(\n\n");
                    this.setStatus("UNREALIZABLE");
                    break;

                case ".inputs":
                    Set<String> chan = new HashSet<>();
                    for (String ch : mParts) {
                        chan.add(ch);
                    }
                    mInt.setChannels(chan);

                    break;
                case ".outputs":
                    Set<String> out = new HashSet<>();
                    for (String o : mParts) {
                        out.add(o);
                    }
                    mInt.setOutput(out);
                    this.setInterface(mInt);
                    break;
                case ".s":
                    int n = Integer.parseInt(mParts[0].trim());
                    for (int i = 0; i < n; i++) {
                        this.states.add(new State(i + ""));
                    }
                    break;
                case ".r":
                    recent = ".r";
                    this.setInitState(this.getStateById(parts[1].substring(1).trim()));
                    break;

                default:
                    if (recent.equals(".r")) {
                        if (parts.length > 4) {
                            parts = processLine(line);
                        }

                        String form = parts[0] + ":" + parts[3];
                        Boolean decision = form.contains("-") | form.contains("+");
                        if (decision) {
                            if (this.status.equals("REALIZABLE")) {
                                for (String act : this.CodeWildCard(form, cCode, oCode)) {
                                    this.transitions.add(new Trans(this.getStateById(parts[1].substring(1).trim()),
                                            act,
                                            this.getStateById(parts[2].substring(1).trim())));
                                    if (this.getStateById(parts[1].substring(1).trim())
                                            .equals(this.getInitState())) {
                                        this.setInitTrans(new Trans(this.getStateById(parts[1].substring(1).trim()),
                                                act,
                                                this.getStateById(parts[2].substring(1).trim())));
                                    }
                                }

                            } else {
                                for (String act : this.CodeWildCard(form, oCode, cCode)) {
                                    this.transitions.add(new Trans(this.getStateById(parts[1].substring(1).trim()),
                                            act,
                                            this.getStateById(parts[2].substring(1).trim())));
                                    if (this.getStateById(parts[1].substring(1).trim())
                                            .equals(this.getInitState())) {
                                        this.setInitTrans(new Trans(this.getStateById(parts[1].substring(1).trim()),
                                                act,
                                                this.getStateById(parts[2].substring(1).trim())));
                                    }
                                }

                            }

                        } else {
                            if (this.status.equals("REALIZABLE")) {
                                this.transitions.add(new Trans(this.getStateById(parts[1].substring(1).trim()),
                                        this.CodeResolve(cCode, parts[0]) + "/"
                                                + this.CodeResolve(oCode, parts[3]),
                                        this.getStateById(parts[2].substring(1).trim())));
                                if (this.getStateById(parts[1].substring(1).trim()).equals(this.getInitState())) {
                                    this.setInitTrans(new Trans(this.getStateById(parts[1].substring(1).trim()),
                                            this.CodeResolve(cCode, parts[0]) + "/"
                                                    + this.CodeResolve(oCode, parts[3]),
                                            this.getStateById(parts[2].substring(1).trim())));
                                }
                            } else {
                                this.transitions.add(new Trans(this.getStateById(parts[1].substring(1).trim()),
                                        this.CodeResolve(oCode, parts[3]) + "/"
                                                + this.CodeResolve(cCode, parts[0]),
                                        this.getStateById(parts[2].substring(1).trim())));
                                if (this.getStateById(parts[1].substring(1).trim()).equals(this.getInitState())) {
                                    this.setInitTrans(new Trans(this.getStateById(parts[1].substring(1).trim()),
                                            this.CodeResolve(oCode, parts[3]) + "/"
                                                    + this.CodeResolve(cCode, parts[0]),
                                            this.getStateById(parts[2].substring(1).trim())));
                                }
                            }
                        }
                    }
                    break;
            }

        }

        reader.close();
        int i=0;
        for (Trans tr : this.getTransitions()) {
            this.code.put(this.IdState(tr), i);
            i+=1;
        }
        System.out.println(this.code.toString());
    }

    public String[] processLine(String line) {
        String[] act = line.split(" ");
        Boolean reached = false;
        String st = "";

        for (String s : act) {
            if (s.contains("S")) {
                reached = true;
            }
            if (reached) {
                if (s.contains("S")) {
                    st += s + ":";
                } else {
                    st += s;
                }

            }
        }

        line = line.substring(0, line.indexOf("S")).replace(" ", "") + ":" + st;

        String[] parts = line.split(":");
        return parts;
    }

    public TS toTS(Mealy m, String name) {
        TS ts = new TS("T[" + name + "]");
        ts.setInterface(m.getInterface());
        for (Trans tr : m.getTransitions()) {
            if (tr.getAction().split("/").length > 1) {
                String[] parts = tr.getAction().split("/");
                State st = new State(this.code.get(IdState(tr)).toString(),
                        new Label(new HashSet<>(Arrays.asList(parts[0].trim().split(","))),
                                new HashSet<>(Arrays.asList(parts[1].trim().split(",")))));
                ts.addState(st);
            } else {
                if (tr.getAction().equals("/")) {
                    State st = new State(this.code.get(IdState(tr)).toString(),
                            new Label(new HashSet<>(Arrays.asList(" ")),
                                    new HashSet<>(Arrays.asList(" "))));
                    ts.addState(st);
                } else {
                    if (tr.getAction().startsWith("/")) {
                        String[] parts = tr.getAction().split("/");
                        State st = new State(this.code.get(IdState(tr)).toString(),
                                new Label(new HashSet<>(Arrays.asList(" ")),
                                        new HashSet<>(Arrays.asList(parts[1].trim().split(",")))));
                        ts.addState(st);
                    }
                    if (tr.getAction().endsWith("/")) {
                        String[] parts = tr.getAction().split("/");
                        State st = new State(this.code.get(IdState(tr)).toString(),
                                new Label(new HashSet<>(Arrays.asList(parts[0].trim().split(","))),
                                        new HashSet<>(Arrays.asList(" "))));
                        ts.addState(st);
                    }
                }

            }

        }
        for (Trans tr_1 : m.getTransitions()) {
            for (Trans tr_2 : m.getTransitions()) {
                if (tr_1.getDestination().equals(tr_2.getSource())) {
                    ts.addTransition(ts,
                            ts.getStateById(this.code.get(IdState(tr_1)).toString()),
                            formatChannel(ts.getStateById(this.code.get(IdState(tr_2)).toString()).getLabel().getChannel()),
                            ts.getStateById(this.code.get(IdState(tr_2)).toString()));

                    ts.getStateById(this.code.get(IdState(tr_1)).toString())
                            .addTrans(new Trans(ts.getStateById(this.code.get(IdState(tr_1)).toString()),
                                    formatChannel(ts.getStateById(this.code.get(IdState(tr_2)).toString()).getLabel().getChannel()),
                                    ts.getStateById(this.code.get(IdState(tr_2)).toString())), ts);

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
        ts.setInitState(m.code.get(IdState(m.getInitTrans())).toString());

        for (State state : ts.getStates()) {

            for (Trans tr1 : state.getTrans()) {
                for (Trans tr2 : state.getTrans()) {
                    if (!tr1.equals(tr2) && tr1.action.equals(tr2.getAction())) {
                        ts.setStatus("ND");
                    }
                }
            }
        }
        ts.toDot();
        return ts;
    }

    public void toDot(Mealy m, String name) {

        Printer gp = new Printer(name);
        gp.addln("\ngraph [rankdir=LR,ranksep=.6,nodesep=0.5];\n");
        gp.addln("\nsubgraph cluster_L { \"\" [shape=box fontsize=16 style=\"filled\" label=\n");
        gp.addln("\"" + m.getInterface().toString());
        gp.addln(
                "\n The generated Mealy Machine\"]}");
        gp.addln("\n\n\n\n");
        gp.addln("node[shape=circle style=filled fixedsize=true fontsize=10]\n");

        gp.addln("init [shape=point,style=invis];");
        for (State state : m.states) {
            gp.addln("\t" + state.getId().toString() + "[label=\"" + state.getId().toString() + "\"]" + "\n");
        }

        gp.addln("\t" + " init -> " + m.getInitState().getId().toString() + "[penwidth=0,tooltip=\"initial state\"]"
                + ";\n");
        for (Trans t : m.getTransitions()) {
            String source = t.getSource().getId().toString();
            String dest = t.getDestination().getId().toString();
            String action = t.getAction().toString();
            gp.addln("\t" + source + " -> " + dest + "[label=\"" + action + "\"]" + ";\n");

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

}
