package com.syntm.lts;
/*
Author:  Yehia Abd Alrahman (yehiaa@chalmers.se)
Listen.java (c) 2024
Desc: Listening function
Created:  17/11/2024 09:45:55
Updated:  17/11/2024 12:45:53
Version:  1.1
*/


import java.util.HashSet;
import java.util.Set;
public class Listen {
    private Set<String> channels;

    public Listen(Set<String> channels) {
        this.channels = channels;
    }

    public Listen() {
        this.channels = new HashSet<String>();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channels == null) ? 0 : channels.hashCode());
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
        Listen other = (Listen) obj;
        if (channels == null) {
            if (other.channels != null)
                return false;
        } else if (!channels.equals(other.channels))
            return false;
        return true;
    }

    public Set<String> getChannels() {
        return channels;
    }

    public void setChannels(Set<String> channels) {
        this.channels = new HashSet<String>(channels);
    }

    @Override
    public String toString() {
        return "" + channels;
    }

    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

}
