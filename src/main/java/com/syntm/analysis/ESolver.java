package com.syntm.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.syntm.lts.CompressedTS;
import com.syntm.lts.State;
import com.syntm.lts.TS;
import com.syntm.lts.Trans;
import com.syntm.util.Printer;

public class ESolver {
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
    int counter = 0;
    boolean fixed = false;
    while (!fixed) {
      counter += 1;
      // System.out.println("\n\n SYNCHRONISATION ROUND#" + counter + "\n\n");
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
          future.get();
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      }
      // System.out.println("Fixed map -> " + eMap);

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
    if (fixedPoint) {
      printFixedRho(eMap);
    }

    for (int i = 0; i < wList.size(); i++) {
      wList.get(i).setlMap(eMap);
    }

    return fixedPoint;
  }

  public Set<Set<State>> buildFinalRho(){
    List<State> keys = new ArrayList<State>();
    HashMap<State,Set<State>> atState = new HashMap<>();
    HashMap<State,Set<State>> preState = new HashMap<>();
    TS paramTS = new TS("");
    paramTS = eMap.keySet().iterator().next().getOwner();
    
    for (State state : this.ts.getStates()) {
      Set<State> temp = new HashSet<>();
      for (String id : state.getqState()) {
       temp.add(paramTS.getStateById(id));
      }
      atState.put(state, temp);
    }

    for (State s : this.ts.getStates()) {
      Set<State> temp = new HashSet<>();
      for (State sPrime : this.ts.getStates()) {
       if (sPrime.getTrans().stream().map(Trans::getDestination).collect(Collectors.toSet()).contains(s)) {
        temp.add(sPrime);
       } ;
      }
    }
    
    for (State epsilon : eMap.keySet()) {
      keys.add(epsilon);
    }
    keys.sort((e1, e2) -> e1.getId().compareTo(e2.getId()));


    return null;
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
      gp.addln("post-> "+epsilon.getPost());
      gp.addln("pre-> "+epsilon.getPre());

    }
    gp.addln("\n === Care of Epsilons === \n");

    wList.sort((e1, e2) -> e1.getEpsilon().getId().compareTo(e2.getEpsilon().getId()));

    for (Task t : wList) {

      final Set<State> temp = t.lMap.keySet()
          .stream()
          .filter(s -> s.getId() != t.getEpsilon().getId()).collect(Collectors.toSet());
     // System.err.println("temp ->" + temp);

      Set<String> ids = temp.stream().map(State::getId).collect(Collectors.toSet());

      ids.add(t.getEpsilon().getId());

      //System.err.println("ids ->" + ids);

     

      Set<Set<State>> cares = new HashSet<>();

       cares = map.get(t.getEpsilon())
      .stream()
      .filter(p -> !p
                      .stream()
                      .filter( s -> ids.contains(s.getId()))
                      .collect(Collectors.toSet()).isEmpty())
      .collect(Collectors.toSet());
      
      

      cares.addAll(map.get(t.getEpsilon())
          .stream()
          .filter(p -> !p
              .stream().filter(s -> s.getId().equals(t.getEpsilon().getId())).collect(Collectors.toSet())
              .isEmpty())
          .collect(Collectors.toSet()));

      gp.addln("\t" + t.getEpsilon().getId() + "-> decides for:\n");
      gp.addln("\t\t Itself: " + t.getEpsilon() + " \n");
      gp.addln("\t\t Its successors: " + " \n");
      gp.addln("\t\t\t"
          + temp
          + " ,\n");
      gp.addln("\t\t Equivalence classes of Intereset for " + t.getEpsilon().getId() + " ->\n");
      gp.addln("\t\t\t"
          + cares
          + " ,\n");
    }
    Set<String> qSet = new HashSet<>();
    
    for (State state : this.ts.getStates()) {
      qSet.addAll(state.getqState());
      gp.addln("\t\t Quotient states for " + state.getId() + " ->\n");
      gp.addln("\t\t\t"
     +state.getId()+ " -:- "+ state.getqState()
      + " ,\n");

      gp.addln("\t\t\t Pre ->"
     +state.getPre()+ " -:- "+ state.getId()
      + " ,\n");

      gp.addln("\t\t\t Post ->"
     +state.getPost()+ " -:- "+ state.getId()
      + " ,\n");
    }
    gp.printText();

  }

}