package com.syntm.lts;

import java.util.Set;

public class Int {
    private Set<String> channels;
    private Set<String> output;

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
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
