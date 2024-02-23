package com.syntm.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.syntm.lts.CompressedTS;
import com.syntm.lts.State;
import com.syntm.lts.TS;

public class ESolver {
  private ConcurrentHashMap<State, Set<Set<State>>> eMap;
  private TS ts;
  private Set<String> channels;
  List<Task> wList;
  private ExecutorService service;

  /**
   * Complexity O(n^2 + m)
   * @param indexedFamily
   * @param t
   * @param sch
   */
  public ESolver(ConcurrentMap<State, Set<Set<State>>> indexedFamily, TS t, Set<String> sch) {
    eMap = new ConcurrentHashMap<>(indexedFamily);
    this.channels = sch;
    this.ts = t;
    wList = new ArrayList<>();
    int corecount = Runtime.getRuntime().availableProcessors();
    service = Executors.newFixedThreadPool(corecount);
    int i = 0;
    for (State s : eMap.keySet()) { // O(n^2 + m)
      Task w = new Task(s.getId(), eMap.get(s), eMap, channels); // O(n + m)
      wList.add(i, w);
      i += i;
      // service.execute(w);
      // new Thread(w).start();
    }

  }

  /**
   * Complexity O(n^2 + cmn^4 + cm^2n^3) perhaps?
   * @return
   */
  public TS run() {
    //int counter = 0;
    do {
     // counter += 1;
       //System.out.println("\n\n SYNCHRONISATION ROUND#" + counter + "\n\n");
      // Execute all tasks and get reference to Future objects
      List<Future<Set<Set<State>>>> resultList = null;
      try {
        resultList = service.invokeAll(wList); // <---- does the things (in parallel)
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      for (int i = 0; i < resultList.size(); i++) { // sync
        Future<Set<Set<State>>> future = resultList.get(i);
        try {
          future.get(); // Used for synchronising
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      }
    } while (!updateMap()); // O(n^2) excluding the parallel stuff
    service.shutdown();

    Set<Set<State>> rhoFinal = new HashSet<>();
    String id = ts.getInitState().getId();
    for (State s : eMap.keySet()) {
      if (s.getId().equals(id)) {
        rhoFinal = new HashSet<>(eMap.get(s));
      }
    }
    if (rhoFinal.size()==1) {
      return this.ts;
    }
    CompressedTS c = new CompressedTS("s-" + this.ts.getName());
    TS t = c.compressedTS(this.ts, rhoFinal);
    return t; 
  }

  /**
   * Complexity O(n)
   * @return
   */
  private boolean updateMap() {
    boolean fixedPoint = true;

    for (int i = 0; i < wList.size(); i++) { // O(n)
      if (!wList.get(i).getRhoEpsilon().equals(wList.get(i).getRhoTemp())) {
        wList.get(i).setRhoEpsilon(wList.get(i).getRhoTemp());
        eMap.put(wList.get(i).getEpsilon(), wList.get(i).getRhoTemp());
        fixedPoint = false;
      }
    }
    // if (fixedPoint) {
    // System.out.println("Fixed map -> " + eMap);
    // }

    for (int i = 0; i < wList.size(); i++) { // O(n)
      wList.get(i).setlMap(eMap);
    }

    return fixedPoint;
  }

}
