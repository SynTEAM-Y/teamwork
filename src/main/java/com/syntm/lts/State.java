package com.syntm.lts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class State implements java.io.Serializable {
	private String id;
	private Label label;
	private Listen listen;
	private Set<Trans> trans;
	private Set<State> comStates;
	private TS owner;
    public Map<State, Integer> blockId;

	public State() {
		this.id = "";
		this.label = new Label();
		this.listen = new Listen();
		this.trans=new HashSet<>();
		this.comStates=new HashSet<>();		
        this.blockId = new HashMap<>();
	}
    
	public State(String id) {
        this.id = id;
		this.label = new Label();
		this.listen = new Listen();
		this.trans=new HashSet<>();
		this.comStates=new HashSet<>();		
        this.blockId = new HashMap<>();
	}
    
	public State(String id, Label label, Listen listen) {
        this.id = id;
		this.label = label;
		this.listen = listen;
		this.trans=new HashSet<>();
		this.comStates=new HashSet<>();
        this.blockId = new HashMap<>();
	}
    
    public State(String id, Label label) {
        this.id = id;
		this.label = label;
		this.listen = new Listen();
		this.trans=new HashSet<>();
		this.comStates=new HashSet<>();
        this.blockId = new HashMap<>();
	}
    
	public State(String id, Label label, Listen listen, Set<Trans> trans) {
        this.id = id;
		this.label = label;
		this.listen = listen;
		this.trans=trans;
		this.comStates=new HashSet<>();
        this.blockId = new HashMap<>();
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
		return " State" + "(id=" + id + ", L=" + label + ", LS=" + listen + ") -->" + blockId + "<--";
	}

    /**
     * Complexity O(1)
     * @return
     */
	public String getId() {
		return id;
	}
    /**
     * Complexity O(1)
     * @return
     */
	public Label getLabel() {
		return label;
	}

    /**
     * Complexity O(1)
     * @param label
     * @return
     */
	public Label setLabel(Label label) {
		this.label = label;
		return label;
	}

    /**
     * Complexity O(1)
     * @return
     */
	public Listen getListen() {
		return listen;
	}

    /**
     * Complexity O(1)
     * @param listen
     * @return
     */
	public Listen setListen(Listen listen) {
		this.listen = listen;
		return listen;
	}
	
    /**
     * Complexity O(1)
     * @return
     */
	public Set<Trans> getTrans() {
		return trans;
	}

    /**
     * Complexity O(1)
     * @param trans
     */
	public void setTrans(Set<Trans> trans) {
		this.trans = trans;
	}

    /**
     * Complexity O(1)
     * @param id
     */
	public void setId(String id) {
		this.id = id;
	}

    /**
     * Complexity O(1)
     * @return
     */
	public Set<State> getComStates() {
		return comStates;
	}

    /**
     * Complexity O(1)
     * @param comStates
     */
	public void setComStates(Set<State> comStates) {
		this.comStates = comStates;
	}

    /**
     * Complexity O(1)
     * @return
     */
	public TS getOwner() {
		return owner;
	}

    /**
     * Complexity O(1)
     * @param owner
     */
	public void setOwner(TS owner) {
		this.owner = owner;
	}

    /**
     * Complexity O(1)
     * @param s The state mapped to the partition.
     * @return The id of the block containing the state in the partition.
     */
    public int getBlockId(State s) {
        return this.blockId.get(s);
    }

    /**
     * Sets a new block id <p>
     * Complexity O(1)
     * @param s The state mapped to the partition.
     * @param id A new id of the block containing the state in the partition.
     */
    public void setBlockId(State s, int id) {
        this.blockId.put(s, id);
    }

    /**
     * Complexity O(1)
     * @param tr
     * @param ts
     */
    public void addTrans(Trans tr, TS ts) {
		tr.getSource().setOwner(ts);
		tr.getDestination().setOwner(ts);
		this.trans.add(tr);
    }

    /**
     * Complexity O(c), combined with an outer <code>for every state</code> becomes O(m)
     * @param t
     * @param s
     * @param ch
     * @return
     */
    public boolean canExactSilent(TS t, State s, String ch) { // O(c)
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

    /**
     * Complexity O(c), combined with an outer <code>for every state</code> becomes O(m)
     * @param t
     * @param s
     * @param ch
     * @return
     */
    public Trans takeExactSilent(TS t, State s, String ch) { // O(c)
        for (Trans tr : s.getTrans()) {
            if (tr.getDestination().getLabel().equals(s.getLabel())
                    && !t.getInterface().getChannels().contains(ch)
                    && tr.getAction().equals(ch)) {
                return tr;
            }
        }
        return null;
    }

    /**
     * Complexity O(c), combined with an outer <code>for every state</code> becomes O(m)
     * @param t
     * @param s
     * @param ch
     * @return
     */
    public boolean canAnotherSilent(TS t, State s, String ch) { // O(c)
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

    /**
     * Complexity O(c), combined with an outer <code>for every state</code> becomes O(m)
     * @param t
     * @param s
     * @param ch
     * @return
     */
    public Trans takeAnotherSilent(TS t, State s, String ch) { // O(c)
        for (Trans tr : s.getTrans()) {
            if (tr.getDestination().getLabel().equals(s.getLabel())
                    && !t.getInterface().getChannels().contains(ch)
                    && !tr.getAction().equals(ch)) {
                return tr;
            }
        }
        return null;
    }

    /**
     * Complexity O(c), combined with an outer <code>for every state</code> becomes O(m)
     * @param t
     * @param s
     * @param ch
     * @return
     */
    public Set<Trans> weakSilent(TS t, State s, String ch) { // O(c)
        Set<Trans> tSet = new HashSet<>();
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
        return tSet; // Max size O(c)
    }

    /**
     * Breadth first search 
     * <p> Complexity O(m + n)
     * @param ts
     * @param s
     * @param ch
     * @return
     */
    public Set<State> weakBFS(TS ts, State s, String ch) { // O(m+n)
        HashMap<State, Boolean> visited = new HashMap<>();
        for (State st : ts.getStates()) { // O(n)
            visited.put(st, false);
        }
        LinkedList<State> queue = new LinkedList<>();
        Set<Trans> transitiveC = new HashSet<>();
        Set<State> reach = new HashSet<>();
        visited.put(s, false);
        queue.add(s);
        while (!queue.isEmpty()) { // O(n+m) because logic
            s = queue.poll();
            reach.add(s);
            transitiveC = weakSilent(ts, s, ch); // O(c)
            if (transitiveC != null && !transitiveC.isEmpty()) {
                for (Trans tr : transitiveC) { // O(c)
                    if (!visited.get(tr.getDestination())) {
                        visited.put(tr.getDestination(), true);
                        queue.add(tr.getDestination());
                    }
                }
            }
        }
        return reach;
    }

    /**
     * Complexity O(c), combined with an outer <code>for every state</code> becomes O(m) because a transition is never checked twice
     * @param t
     * @param s
     * @param ch
     * @return
     */
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

    /**
     * Complexity O(c), combined with an outer <code>for every state</code> becomes O(m)
     * @param t
     * @param s
     * @param ch
     * @return
     */
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

    /**
     * Complexity O(c), combined with an outer <code>for every state</code> becomes O(m+n)
     * @param t
     * @param s
     * @param ch
     * @return
     */
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

    /**
     * Complexity O(c), combined with an outer <code>for every state</code> becomes O(m+n)
     * @param t
     * @param s
     * @param ch
     * @return
     */
    public Trans takeInitiative(TS t, State s, String ch) {
        for (Trans tr : s.getTrans()) {
            if (t.getInterface().getChannels().contains(ch)
                    && tr.getAction().equals(ch)) {
                return tr;
            }
        }
        return null;
    }

    /**
     * Complexity O(c), combined with an outer <code>for every state</code> becomes O(m+n)
     * @param s
     * @param ch
     * @return
     */
    public boolean enable(State s, String ch) {
        for (Trans tr : s.getTrans()) {
            if (tr.getAction().equals(ch)) {
                return true;
            }
        }
        return false;
    }
}
