/*
Author:  Yehia Abd Alrahman (yehiaa@chalmers.se)
Pathn.java (c) 2025
Desc: description
Created:  2025-05-30T11:14:29.315Z
Updated:  30/05/2025 17:12:10
Version:  1.1
*/

package com.syntm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.*;

import org.apache.commons.math3.util.Combinations;

import com.syntm.lts.Int;
import com.syntm.lts.Label;
import com.syntm.lts.State;
import com.syntm.lts.TS;
import com.syntm.lts.Trans;

public class Pathn {

    public static void main(final String[] args) {
        Pathn p = new Pathn();
        p.buildPathn(10);
    }

    public void buildPathn(int size) {
        Combinations combinations = new Combinations(size, 2);
        TS ts = new TS("Path"+size);
        State sdummy = new State("00");
        sdummy.setLabel(new Label(new HashSet<>(), new HashSet<>()));
	    ts.addState(sdummy);
		ts.setInitState(sdummy.getId());
       
        Set<int[]> comb = new HashSet<>();
        Set<int[]> combw = new HashSet<>();
        for (int[] is : combinations) {
            comb.add(is);
            State s = new State(is[0] + "" + is[1]);
            ts.addState(s);
            sdummy.addTrans(new Trans(sdummy, s.getId(), s), ts);
            sdummy.getListen().getChannels().add(is[0] + "" + is[1]);
            ts.addTransition(ts, sdummy, s.getId(), s);
        }
        ts.setInterface(new Int(ts.getInitState().getListen().getChannels(), new HashSet<>()));
        for (int[] list : comb) {
            combw.addAll(comb);
            combw.removeAll(Arrays.asList(list));
            Set<int[]> target = combw.stream().filter(
                    a -> (a[0] == list[0] || a[1] == list[0] || a[0] == list[1] || a[1] == list[1]))
                    .collect(Collectors.toSet());
            for (int[] is : target) {
                //System.err.println(is[0] + "" + is[1]);
                ts.getStateById(list[0] + "" + list[1]).addTrans(new Trans(ts.getStateById(list[0] + "" + list[1]), is[0] + "" + is[1], ts.getStateById(is[0] + "" + is[1])), ts);
                ts.addTransition(ts, ts.getStateById(list[0] + "" + list[1]), is[0] + "" + is[1], ts.getStateById(is[0] + "" + is[1]));
                ts.getStateById(list[0] + "" + list[1]).getListen().getChannels().add(is[0] + "" + is[1]);
            }
            ts.getStateById(list[0] + "" + list[1]).setLabel(new Label(new HashSet<>(Arrays.asList(list[0] + "" + list[1])), new HashSet<>()));
            
           
        }
      
        ts.toDot();

    }
}