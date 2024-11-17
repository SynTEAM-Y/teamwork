package com.syntm.lts;
/*
Author:  Yehia Abd Alrahman (yehiaa@chalmers.se)
Label.java (c) 2024
Desc: State label
Created:  17/11/2024 09:45:55
Updated:  17/11/2024 12:46:00
Version:  1.1
*/


import java.util.HashSet;
import java.util.Set;
public class Label {
    private Set<String> channel;
    private Set<String> output;

    public Label(Set<String> channel, Set<String> output) {
        this.channel = channel;
        this.output = output;
    }

    public Label() {
        channel = new HashSet<String>();
        output = new HashSet<String>();
    }

    public Set<String> getChannel() {
        return channel;
    }

    public void setChannel(Set<String> channel) {
        this.channel = channel;
    }

    public Set<String> getOutput() {
        return output;
    }

    public void setOutput(Set<String> output) {
        this.output = output;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channel == null) ? 0 : channel.hashCode());
        result = prime * result + ((output == null) ? 0 : output.hashCode());
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
        Label other = (Label) obj;
        if (channel == null) {
            if (other.channel != null)
                return false;
        } else if (!channel.equals(other.channel))
            return false;
        if (output == null) {
            if (other.output != null)
                return false;
        } else if (!output.equals(other.output))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return channel + "/" + output;
    }
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
