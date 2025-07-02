/*
Author:  Yehia Abd Alrahman (yehiaa@chalmers.se)
Pathn.java (c) 2025
Desc: description
Created:  2025-05-30T11:14:29.315Z
Updated:  02/07/2025 21:20:08
Version:  1.1
*/

package com.syntm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.*;

import org.apache.commons.math3.util.Combinations;
import org.apache.commons.math3.util.MathUtils;
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
            map.put("" + i, new HashSet<>());
        }

        Boolean flag = true;
        ArrayList<int[]> scha = new ArrayList<>(comb);
        scha.sort((e1, e2) -> e1[1] - e2[1]);
        scha.sort((e1, e2) -> e1[0] - e2[0]);
        while (flag) {
            int[] cha = scha.iterator().next();
            scha.remove(cha);

            List<String> keysa = map.keySet().stream()
                    .filter(k -> ((Arrays.stream(cha).filter(x -> x == Integer.parseInt(k)).toArray()).length != 0)
                            && (map.get(k).size() < Math.ceilDiv(ts.getInterface().getChannels().size(),
                                    size)))
                    .collect(Collectors.toList());

            map.get(keysa.get(0)).add(cha[0] + "" + cha[1]);
            if (scha.isEmpty()) {
                flag = false;
            }
        }
        for (int i = 0; i < size; i++) {
            pr.add("P" + i + ": CH=" + map.get(i + "").toString().replaceAll("\\s+", "") + ", OUT=[-]");
            if (i != size - 1) {
                pr.addln(";\n");
            }
        }
        pr.add("\"];\n");
        System.out.println(pr.getsBuilder().toString());
        ts.toDot(pr);

    }
}