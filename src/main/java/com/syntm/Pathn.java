/*
Author:  Yehia Abd Alrahman (yehiaa@chalmers.se)
Pathn.java (c) 2025
Desc: description
Created:  2025-05-30T11:14:29.315Z
Updated:  31/05/2025 00:03:54
Version:  1.1
*/

package com.syntm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.*;

import org.apache.commons.math3.util.Combinations;

import com.syntm.lts.Int;
import com.syntm.lts.Label;
import com.syntm.lts.State;
import com.syntm.lts.TS;
import com.syntm.lts.Trans;
import com.syntm.util.Printer;

public class Pathn {

    public static void main(final String[] args) {
        Pathn p = new Pathn();
        p.buildPathn(3);
    }

    public void buildPathn(int size) {
        Combinations combinations = new Combinations(size, 2);
        TS ts = new TS("Path" + size);
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
                ts.getStateById(list[0] + "" + list[1]).addTrans(new Trans(ts.getStateById(list[0] + "" + list[1]),
                        is[0] + "" + is[1], ts.getStateById(is[0] + "" + is[1])), ts);
                ts.addTransition(ts, ts.getStateById(list[0] + "" + list[1]), is[0] + "" + is[1],
                        ts.getStateById(is[0] + "" + is[1]));
                ts.getStateById(list[0] + "" + list[1]).getListen().getChannels().add(is[0] + "" + is[1]);
            }
            ts.getStateById(list[0] + "" + list[1])
                    .setLabel(new Label(new HashSet<>(Arrays.asList(list[0] + "" + list[1])), new HashSet<>()));
        }
        Printer pr = new Printer("d");
        pr.addln(
                "spec [fontcolor=\"green\",fontsize=14,peripheries=0,shape=square,fixedsize=false,style=\"\",label=\"Distribute to:\n");
        HashMap<String, Set<String>> map = new HashMap<>();

        for (int i = 0; i < size; i++) {
            map.put("P" + i, new HashSet<>());
            pr.add("P" + i + " : CH=" + ts.getInterface().getChannels() + ", OUT=[-]");
            if (i != size - 1) {
                pr.addln(";\n");
            }
        }
        Boolean flag = true;
        Set<String> sch = new HashSet<>(ts.getInterface().getChannels());
        while (flag) {
            String ch= sch.
        }
        pr.add("\"];\n");
        System.out.println(pr.getsBuilder().toString());
        ts.toDot();

    }
}