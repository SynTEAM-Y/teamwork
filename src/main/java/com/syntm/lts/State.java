package com.syntm.lts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class State {
    private String id;
    private Label label;
    private Listen listen;
    private Set<Trans> trans;
    private Set<State> comStates;
    private TS owner;
    private Set<String> qState = new HashSet<>();
    private Set<State> post = new HashSet<>();
    private Set<State> pre = new HashSet<>();

    public State() {
        this.id = "";
        this.label = new Label();
        this.listen = new Listen();
        this.trans = new HashSet<Trans>();
        this.comStates = new HashSet<State>();
    }

    public Set<State> getPost() {
        return post;
    }

    public void setPost(Set<State> post) {
        this.post = post;
    }

    public Set<State> getPre() {
        return pre;
    }

    public void setPre(Set<State> pre) {
        this.pre = pre;
    }

    public State(String id) {
        this.id = id;
        this.label = new Label();
        this.listen = new Listen();
        this.trans = new HashSet<Trans>();
        this.comStates = new HashSet<State>();
    }

    public State(String id, Label label, Listen listen) {
        this.id = id;
        this.label = label;
        this.listen = listen;
        this.trans = new HashSet<Trans>();
        this.comStates = new HashSet<State>();
    }

    public State(String id, Label label) {
        this.id = id;
        this.label = label;
        this.listen = new Listen();
        this.trans = new HashSet<Trans>();
        this.comStates = new HashSet<State>();
    }

    public State(String id, Label label, Listen listen, Set<Trans> trans) {
        this.id = id;
        this.label = label;
        this.listen = listen;
        this.trans = trans;
        this.comStates = new HashSet<State>();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((listen == null) ? 0 : listen.hashCode());

        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
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
        State other = (State) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        if (listen == null) {
            if (other.listen != null)
                return false;
        } else if (!listen.equals(other.listen))
            return false;
        if (owner == null) {
            if (other.owner != null)
                return false;
        } else if (!owner.equals(other.owner))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return " State" + "(id=" + id + ", L=" + label + ", LS=" + listen + ")";
    }

    public String getId() {
        return id;
    }

    public Label getLabel() {
        return label;
    }

    public Label setLabel(Label label) {
        this.label = label;
        return label;
    }

    public Listen getListen() {
        return listen;
    }

    public Listen setListen(Listen listen) {
        this.listen = listen;
        return listen;
    }

    public Set<Trans> getTrans() {
        return trans;
    }

    public void setTrans(Set<Trans> trans) {
        this.trans = trans;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<State> getComStates() {
        return comStates;
    }

    public void setComStates(Set<State> comStates) {
        this.comStates = comStates;
    }

    public TS getOwner() {
        return owner;
    }

    public void setOwner(TS owner) {
        this.owner = owner;
    }

    public void addTrans(Trans tr, TS ts) {
        tr.getSource().setOwner(ts);
        tr.getDestination().setOwner(ts);
        this.trans.add(tr);
    }

    public boolean canExactSilent(TS t, State s, String ch) {
        boolean flag = false;
        if (t.getInterface().getChannels().contains(ch)) {
            return false;
        }
        for (Trans tr : s.getTrans()) {
            if (tr.getDestination().getLabel().equals(s.getLabel())
                    && tr.getAction().equals(ch)) {
                flag = true;
            }

        }
        return flag;
    }

    public Trans takeExactSilent(TS t, State s, String ch) {
        for (Trans tr : s.getTrans()) {
            if (tr.getDestination().getLabel().equals(s.getLabel())
                    && !t.getInterface().getChannels().contains(ch)
                    && tr.getAction().equals(ch)) {
                return tr;
            }
        }
        return null;
    }

    public boolean canAnotherSilent(TS t, State s, String ch) {
        boolean flag = false;
        if (t.getInterface().getChannels().contains(ch)) {
            return false;
        }
        for (Trans tr : s.getTrans()) {
            if (tr.getDestination().getLabel().equals(s.getLabel())
                    && !tr.getAction().equals(ch)) {
                flag = true;
            }

        }
        return flag;
    }

    public Trans takeAnotherSilent(TS t, State s, String ch) {
        for (Trans tr : s.getTrans()) {
            if (tr.getDestination().getLabel().equals(s.getLabel())
                    && !t.getInterface().getChannels().contains(ch)
                    && !tr.getAction().equals(ch)) {
                return tr;
            }
        }
        return null;
    }

    public Set<Trans> weakSilent(TS t, State s, String ch) {
        Set<Trans> tSet = new HashSet<Trans>();
        for (Trans tr : s.getTrans()) {
            if (tr.getAction().equals(ch)) {
                return null;
            }
            if (tr.getDestination().getLabel().equals(s.getLabel())
                    && !t.getInterface().getChannels().contains(ch)
                    && !tr.getAction().equals(ch)) {
                tSet.add(tr);
            }

        }
        return tSet;
    }

    public Set<Trans> TakeDiff(TS t, State s, String ch) {
        Set<Trans> tSet = new HashSet<Trans>();
        for (Trans tr : s.getTrans()) {
            if (tr.getAction().equals(ch)) {
                return new HashSet<>();
            }
            if (!t.getInterface().getChannels().contains(ch)
                    && !tr.getAction().equals(ch)) {
                tSet.add(tr);
            }

        }
        return tSet;
    }

    public Set<State> weakBFS(TS ts, State s, String ch) {
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
            transitiveC = weakSilent(ts, s, ch);
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

    public boolean canDirectReaction(TS t, State s, String ch) {
        boolean flag = false;
        if (t.getInterface().getChannels().contains(ch)) {
            return false;
        }
        for (Trans tr : s.getTrans()) {
            if (!tr.getDestination().getLabel().equals(s.getLabel())
                    && tr.getAction().equals(ch)) {
                flag = true;
            }

        }
        return flag;
    }

    public boolean canAnyReaction(TS t, State s, String ch) {
        boolean flag = false;
        if (t.getInterface().getChannels().contains(ch)) {
            return false;
        }
        if (s.canDirectReaction(t, s, ch) || s.canExactSilent(t, s, ch)) {
            flag = true;
        }

        return flag;
    }

    public Trans takeAnyReaction(TS t, State s, String ch) {
        if (s.canExactSilent(t, s, ch)) {
            return s.takeExactSilent(t, s, ch);
        }
        if (s.canDirectReaction(t, s, ch)) {
            return s.takeDirectReaction(t, s, ch);
        }
        return null;
    }

    public Trans takeDirectReaction(TS t, State s, String ch) {
        for (Trans tr : s.getTrans()) {
            if (!tr.getDestination().getLabel().equals(s.getLabel())
                    && tr.getAction().equals(ch)
                    && !t.getInterface().getChannels().contains(ch)) {
                return tr;
            }
        }
        return null;
    }

    public boolean canTakeInitiative(TS t, State s, String ch) {
        boolean flag = false;
        if (t.getInterface().getChannels().contains(ch)) {
            for (Trans tr : s.getTrans()) {
                if (tr.getAction().equals(ch)) {
                    flag = true;
                }
            }
            return flag;
        }
        return false;
    }

    public Trans takeInitiative(TS t, State s, String ch) {
        for (Trans tr : s.getTrans()) {
            if (t.getInterface().getChannels().contains(ch)
                    && tr.getAction().equals(ch)) {
                return tr;
            }
        }
        return null;
    }

    public Boolean enable(State s, String ch) {
        for (Trans tr : s.getTrans()) {
            if (tr.getAction().equals(ch)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getqState() {
        return qState;
    }

    public void setqState(Set<String> qState) {
        this.qState = qState;
    }
}
