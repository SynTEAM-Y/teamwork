package com.syntm.lts;

import java.util.HashSet;
import java.util.Set;

public class PartitionState {
    private String id;
	private Label label;
	private Listen listen;
    private Set<State> partition;
	private Set<PartitionTrans> PartitionTrans;
    private Set<PartitionState> comStates;
    private CompressedTS owner;
    

	public PartitionState() {
		this.id = "";
		this.label = new Label();
		this.listen = new Listen();
		this.PartitionTrans=new HashSet<PartitionTrans>();
        this.partition=new HashSet<State>();
        this.comStates=new HashSet<PartitionState>();
	}

	public PartitionState(String id) {
		this.id = id;
		this.label = new Label();
		this.listen = new Listen();
		this.PartitionTrans=new HashSet<PartitionTrans>();
        this.partition=new HashSet<State>();
        this.comStates=new HashSet<PartitionState>();
	}

	public PartitionState(String id, Label label, Listen listen) {
		this.id = id;
		this.label = label;
		this.listen = listen;
		this.PartitionTrans=new HashSet<PartitionTrans>();
        this.partition=new HashSet<State>();
        this.comStates=new HashSet<PartitionState>();
	}
	public PartitionState(String id, Label label, Listen listen, Set<PartitionTrans> PartitionTrans) {
		this.id = id;
		this.label = label;
		this.listen = listen;
		this.PartitionTrans=PartitionTrans;
        this.partition=new HashSet<State>();
        this.comStates=new HashSet<PartitionState>();
	}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Set<PartitionTrans> getPartitionTrans() {
        return PartitionTrans;
    }

    public void setPartitionTrans(Set<PartitionTrans> PartitionTrans) {
        this.PartitionTrans = PartitionTrans;
    }
    
    

    public Set<State> getPartition() {
        return partition;
    }

    public void setPartition(Set<State> partition) {
        this.partition = partition;
    }

   
    

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((listen == null) ? 0 : listen.hashCode());
        result = prime * result + ((partition == null) ? 0 : partition.hashCode());
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
        PartitionState other = (PartitionState) obj;
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
        if (partition == null) {
            if (other.partition != null)
                return false;
        } else if (!partition.equals(other.partition))
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
        return "PartitionState [id=" + id + ", label=" + label + ", listen=" + listen + ", partition=" + partition+ "]";
    }

    public Set<PartitionState> getComStates() {
        return comStates;
    }

    public void setComStates(Set<PartitionState> comStates) {
        this.comStates = comStates;
    }

    public CompressedTS getOwner() {
        return owner;
    }

    public void setOwner(CompressedTS owner) {
        this.owner = owner;
    }

    public State toState() {
        State state = new State(this.getId(),this.getLabel(),this.getListen());
        return state;
    }
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
	
}
