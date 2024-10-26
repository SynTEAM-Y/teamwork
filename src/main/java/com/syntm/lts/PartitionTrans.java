package com.syntm.lts;

public class PartitionTrans implements Comparable<PartitionTrans> {
    public PartitionState source;
    public String action;
    public PartitionState destination;
    public PartitionTrans(PartitionState source, String action, PartitionState destination) {
        this.source = source;
        this.action = action;
        this.destination = destination;
        
    }
    public PartitionState getSource() {
        return source;
    }
    public void setSource(PartitionState source) {
        this.source = source;
    }
    public String getAction() {
        return action;
    }
    public void setAction(String action) {
        this.action = action;
    }
    public PartitionState getDestination() {
        return destination;
    }
    public void setDestination(PartitionState destination) {
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
        PartitionTrans other = (PartitionTrans) obj;
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
    public int compareTo(PartitionTrans o) {
        return this.toString().compareTo(o.toString());
    }
    @Override
    public String toString() {
        return "PartitionTrans [source=" + source + ", action=" + action + ", destination=" + destination + "]";
    }
    public Trans toTrans() {
        Trans tr = new Trans(this.getSource().toState(), this.getAction(), this.getDestination().toState());
        return tr;
    }
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
