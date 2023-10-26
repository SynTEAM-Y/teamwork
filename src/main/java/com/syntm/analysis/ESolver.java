package com.syntm.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.syntm.lts.CompressedTS;
import com.syntm.lts.State;
import com.syntm.lts.TS;

public class ESolver {
  private Task task;
  private ConcurrentHashMap<State, Set<Set<State>>> eMap;
  private TS ts;
  private Set<String> channels;
  List<Task> wList;
  private ExecutorService service;

  public ESolver(ConcurrentHashMap<State, Set<Set<State>>> indexedFamily, TS t, Set<String> sch) {
    eMap = new ConcurrentHashMap<>(indexedFamily);
    this.channels = sch;
    this.ts = t;
    wList = new ArrayList<Task>();
    int corecount = Runtime.getRuntime().availableProcessors();
    service = (ExecutorService) Executors.newFixedThreadPool(corecount);
    int i = 0;
    for (State s : eMap.keySet()) {
      Task w = new Task(s.getId(), eMap.get(s), eMap, channels);
      wList.add(i, w);
      i += i;
      // service.execute(w);
      // new Thread(w).start();
    }

  }

  public TS run() {
    //int counter = 0;
    boolean fixed = false;
    while (!fixed) {
     // counter += 1;
       //System.out.println("\n\n SYNCHRONISATION ROUND#" + counter + "\n\n");
      // Execute all tasks and get reference to Future objects
      List<Future<Set<Set<State>>>> resultList = null;
      try {
        resultList = service.invokeAll(wList);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      for (int i = 0; i < resultList.size(); i++) {
        Future<Set<Set<State>>> future = resultList.get(i);
        try {
          Set<Set<State>> result = future.get();
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      }
      fixed = updateMap();
    }
    service.shutdown();

    Set<Set<State>> rho_f = new HashSet<>();
    String id = ts.getInitState().getId();
    for (State s : eMap.keySet()) {
      if (s.getId().equals(id)) {
        rho_f = new HashSet<>(eMap.get(s));
      }
    }
    if (rho_f.size()==1) {
      return this.ts;
    }
    CompressedTS c = new CompressedTS("s-" + this.ts.getName());
    TS t = c.compressedTS(this.ts, rho_f);
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
    // System.out.println("Fixed map -> " + eMap);
    // }

    for (int i = 0; i < wList.size(); i++) {
      wList.get(i).setlMap(eMap);
    }

    return fixedPoint;
  }

}