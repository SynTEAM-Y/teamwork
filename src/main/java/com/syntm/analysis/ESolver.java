package com.syntm.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.stream.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.syntm.lts.CompressedTS;
import com.syntm.lts.State;
import com.syntm.lts.TS;
import com.syntm.util.Printer;

public class ESolver {
  private ConcurrentHashMap<State, Set<Set<State>>> eMap;
  private TS ts;
  private TS p;
  private Set<String> channels;
  List<Task> wList;
  private ExecutorService service;

  public ESolver(ConcurrentHashMap<State, Set<Set<State>>> indexedFamily, TS t, TS p, Set<String> sch) {
    eMap = new ConcurrentHashMap<>(indexedFamily);
    this.channels = sch;
    this.ts = t;
    this.p = p;
    wList = new ArrayList<Task>();
    int corecount = Runtime.getRuntime().availableProcessors();
    service = (ExecutorService) Executors.newFixedThreadPool(corecount);
    int i = 0;
    for (State s : eMap.keySet()) {
      Task w = new Task(s.getId(), eMap.get(s), eMap, channels);
      wList.add(i, w);
      i += i;
    }

  }

  public TS run() {
    // int counter = 0;
    boolean fixed = false;
    while (!fixed) {
      // counter += 1;
      // System.out.println("\n\n SYNCHRONISATION ROUND#" + counter + "\n\n");
      // Execute all tasks and get reference to Future objects
      List<Future<Set<Set<State>>>> resultList = new ArrayList<>();
      try {
        resultList = service.invokeAll(wList);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      
      for (int i = 0; i < resultList.size(); i++) {
        Future<Set<Set<State>>> future = resultList.get(i);
        try {
          future.get();
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      }
      // System.out.println("current map -> " + eMap);

      fixed = updateMap();
    }
    service.shutdown();

    Set<Set<State>> rho_f = new HashSet<>();
    rho_f = buildFinalRho();

    if (rho_f.size() == 1) {
      return this.ts;
    }
    CompressedTS c = new CompressedTS("s-" + this.ts.getName());
    TS t = c.DoCompress(this.ts, rho_f);
    return t;
  }

  private boolean updateMap() {
    boolean fixedPoint = true;

    for (int i = 0; i < wList.size(); i++) {
      if (!wList.get(i).getRho_epsilon().equals(wList.get(i).getRho_temp())) {
        wList.get(i).setRho_epsilon(wList.get(i).getRho_temp());
        eMap.put(wList.get(i).getEpsilon(), wList.get(i).getRho_temp());
        fixedPoint = false;
      }
    }
    // if (fixedPoint) {
    // printFixedRho(eMap);
    // }

    for (int i = 0; i < wList.size(); i++) {
      wList.get(i).setlMap(eMap);
    }

    return fixedPoint;
  }

  public Set<Set<State>> buildFinalRho() {
    Set<State> closed = new HashSet<>();
    Set<Set<State>> rho_intersect = new HashSet<>(eMap.get(p.getInitState()));

    HashMap<String, Set<State>> cares = new HashMap<>();

    Set<String> ids = new HashSet<>();
    ids = ts.getStates().stream().map(State::getId).collect(Collectors.toSet());

    for (State state : eMap.keySet()) {
      if (!state.equals(p.getInitState())) {
        rho_intersect.retainAll(eMap.get(state));
      }
    }

    for (String st : ids) {
      Set<State> states = eMap.get(p.getStateById(st)).stream().filter(p -> p.contains(ts.getStateById(st)))
          .collect(Collectors.toSet()).iterator().next();
      cares.put(st, states);

    }

    if (rho_intersect.equals(eMap.get(p.getInitState()))) {
      return rho_intersect;
    }
    rho_intersect.clear();

    for (String s : ids) {
      if (!closed.contains(ts.getStateById(s))) {
        Set<State> b = new HashSet<>();
        b = cares.get(s);
        Set<Set<State>> interSet = new HashSet<>();
        for (State sPrime : b) {
          if (!sPrime.getId().equals(s)) {
            Set<State> pWise = new HashSet<>();
            pWise.addAll(b);
            if (pWise.retainAll(cares.get(sPrime.getId()))) {
              if (!pWise.isEmpty()) {
                interSet.add(pWise);
              }
            }
          }
        }
        Set<State> unionBi = new HashSet<>();
        for (Set<State> pw : interSet) {
          unionBi.addAll(pw);
        }
        b.removeAll(unionBi);
        if (b.isEmpty()) {
          rho_intersect.addAll(interSet);
          closed.addAll(unionBi);
        } else {
          rho_intersect.addAll(interSet);
          rho_intersect.add(b);
          closed.addAll(unionBi);
          closed.addAll(b);
        }
      }
    }
   // System.err.println("rho final" + rho_intersect);
    return rho_intersect;
  }

  public void printFixedRho(ConcurrentHashMap<State, Set<Set<State>>> map) {
    Printer gp = new Printer(this.ts.getName() + "'s Fixed Rho");
    gp.addln("\n An e-Cooperative Bisimulation for " + this.ts.getName() + "\n");
    List<State> keys = new ArrayList<State>();

    for (State epsilon : map.keySet()) {
      keys.add(epsilon);
    }
    keys.sort((e1, e2) -> e1.getId().compareTo(e2.getId()));

    for (State epsilon : keys) {
      gp.addln("\t" + epsilon.getId() + " ->");
      for (Set<State> set : map.get(epsilon)) {
        gp.addln("\t\t" + set);
      }
      gp.addln("\n\t" + "post-> " + epsilon.getPost() + "\n");
      gp.addln("\t" + "pre-> " + epsilon.getPre() + "\n");

    }
    // wList.sort((e1, e2) ->
    // e1.getEpsilon().getId().compareTo(e2.getEpsilon().getId()));
    gp.printText();

  }

}