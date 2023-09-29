package com.syntm.lts;

public class Trans implements Comparable<Trans> {
    public State source;
    public String action;
    public State destination;

    public Trans(State source, String action, State destination) {
        this.source = source;
        this.action = action;
        this.destination = destination;
    }

    public Trans() {
        this.source = new State("");
        this.action = "";
        this.destination = new State("");
    }

    public State getSource() {
        return source;
    }

    public void setSource(State source) {
        this.source = source;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public State getDestination() {
        return destination;
    }

    public void setDestination(State destination) {
        this.destination = destination;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result + ((action == null) ? 0 : action.hashCode());
        result = prime * result + ((destination == null) ? 0 : destination.hashCode());
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
        Trans other = (Trans) obj;
        if (source == null) {
            if (other.source != null)
                return false;
        } else if (!source.equals(other.source))
            return false;
        if (action == null) {
            if (other.action != null)
                return false;
        } else if (!action.equals(other.action))
            return false;
        if (destination == null) {
            if (other.destination != null)
                return false;
        } else if (!destination.equals(other.destination))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("(state(%s),%s,state(%s))", this.source.getId(), this.action, this.destination.getId());
    }

    @Override
    public int compareTo(final Trans o) {
        return this.toString().compareTo(o.toString());
    }
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
    
}
