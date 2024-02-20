package com.syntm.lts;

import java.util.HashSet;
import java.util.Set;

public class Int implements java.io.Serializable {
    private Set<String> channels;
    private Set<String> output;

    public Int() {
        this.channels = new HashSet<>();
        this.output = new HashSet<>();
    }
    public Int(Set<String> channels, Set<String> output) {
        this.channels = channels;
        this.output = output;
    }

    @Override
    public String toString() {
        return "Interface: [channels=" + channels + ", output=" + output + "]";
    }

    public Set<String> getChannels() {
        return channels;
    }

    public Set<String> getOutput() {
        return output;
    }
    
    public void setChannels(Set<String> channels) {
        this.channels = channels;
    }

    public void setOutput(Set<String> output) {
        this.output = output;
    }

    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
