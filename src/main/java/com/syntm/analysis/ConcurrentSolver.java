package com.syntm.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.syntm.lts.CompressedTS;
import com.syntm.lts.State;
import com.syntm.lts.TS;
import com.syntm.lts.Trans;
import com.syntm.util.Printer;

public class ConcurrentSolver {
  private class Worker implements Runnable {
    State epsilon;
    Set<Set<State>> rho_epsilon;
    Set<Set<State>> rho_temp;
    Set<String> channels;
    ConcurrentHashMap<State, Set<Set<State>>> lMap;

    Worker(String e, Set<Set<State>> rho_Set, ConcurrentHashMap<State, Set<Set<State>>> eMap,
        Set<String> channels) {
      this.channels = new HashSet<String>(channels);
      for (State state : eMap.keySet()) {
        if (state.getId().equals(e)) {
          this.epsilon = state;
          break;
        }
      }
      this.rho_epsilon = new HashSet<>(rho_Set);

      this.rho_temp = new HashSet<>(rho_Set);
      this.lMap = new ConcurrentHashMap<>();
      for (Trans tr_e : epsilon.getTrans()) {
        Set<Set<State>> rho = new HashSet<>(eMap.get(tr_e.getDestination()));
        this.lMap.put(tr_e.getDestination(), rho);
      }

    }

    public void run() {
      try {
        while (true) {
          for (String ch : channels) {

            for (Trans trEpsilon : epsilon.getTrans()) {
              Set<Set<State>> ePartitions = new HashSet<>(this.lMap.get(trEpsilon.getDestination()));
              for (Set<State> ePrime : ePartitions) {
                HashMap<Set<State>, Set<State>> splitters = new HashMap<>();
                for (Set<State> partition : this.rho_temp) {
                  if (partition.size() > 1) {
                    Set<State> splitter = applyEBisim(partition, trEpsilon, ePrime, ch);
                    if (!splitter.isEmpty() && !splitter.equals(partition)) {
                      splitters.put(partition, splitter);
                    }
                  }
                }
                if (!splitters.isEmpty()) {
                  for (Set<State> p : splitters.keySet()) {
                    Set<Set<State>> splitP = split(p, splitters.get(p));
                    rho_temp.remove(p);
                    rho_temp.addAll(splitP);
                  }
                }
              }
            }
          }
          roundBarrier.await();
          updateBarrier.await();
        }
      } catch (InterruptedException ex) {
        return;
      } catch (BrokenBarrierException ex) {
        return;
      }
    }

    public State getEpsilon() {
      return epsilon;
    }

    public Set<Set<State>> getRho_epsilon() {
      return new HashSet<>(rho_epsilon);
    }

    public void setRho_epsilon(Set<Set<State>> rho_epsilon) {
      this.rho_epsilon = new HashSet<>(rho_epsilon);
    }

    public Set<Set<State>> getRho_temp() {
      return new HashSet<>(rho_temp);
    }

    public void setlMap(ConcurrentHashMap<State, Set<Set<State>>> lMap) {
      for (Trans tr_e : this.epsilon.getTrans()) {
        Set<Set<State>> rho = new HashSet<>(lMap.get(tr_e.getDestination()));
        this.lMap.put(tr_e.getDestination(), rho);
      }
    }

    private Set<State> applyEBisim(Set<State> p, Trans trEpsilon, Set<State> ePrime, String channel) {
      Set<State> out = new HashSet<State>();
      // Parameter invovled
      if (epsilon.enable(epsilon, channel)
          && trEpsilon.getAction().equals(channel)) {

        if (epsilon.getOwner().getInterface().getChannels().contains(channel)) {
          // reaction by s
          out = sAnyToE(p, ePrime, out, channel);
        }
        // initiation by s
        if (out.isEmpty()) {
          out = sInitiateToE(p, ePrime, out, channel);
        }
      }

      // Parameter is not involved
      if (out.isEmpty() && !epsilon.getListen().getChannels().contains(channel)) {
        // Epsilon does not participate
        out = sInitiateAlone(p, out, channel);
      }

      return out;
    }

    private Set<State> sAnyToE(Set<State> p, Set<State> ePrime, Set<State> out, String channel) {
      for (State s : p) {
        // 3.c
        if (s.canAnyReaction(s.getOwner(), s, channel)) {
          if (ePrime.contains(s.takeAnyReaction(s.getOwner(), s,
              channel).getDestination())) {
            out.add(s);
          }
        }
      }
      Set<State> pPrime = new HashSet<>();
      pPrime.addAll(p);
      pPrime.removeAll(out);
      if (!out.isEmpty()) {
        for (State s : pPrime) {
          if (!s.canAnyReaction(s.getOwner(), s, channel)) {
            if (!s.getListen().getChannels().contains(channel)) {
              {
                out.add(s);
              }
            }
          }
        }
      }

      return out;
    }

    private Set<State> sInitiateToE(Set<State> p, Set<State> ePrime, Set<State> out, String channel) {
      if (!epsilon.canTakeInitiative(epsilon.getOwner(), epsilon, channel)
          && epsilon.getListen().getChannels().contains(channel)) {
        for (State s : p) {
          if (s.canTakeInitiative(s.getOwner(), s, channel)) {
            if (ePrime.contains(s.takeInitiative(s.getOwner(), s, channel).getDestination())) {
              out.add(s);
            }
          }
        }
      }
      return out;
    }

    private Set<State> sInitiateAlone(Set<State> p, Set<State> out, String channel) {
      for (Set<State> partition : rho_epsilon) { // all in same partition.
        for (State s : p) {
          if (s.canTakeInitiative(s.getOwner(), s, channel)) {
            if (partition.contains(s.takeInitiative(s.getOwner(), s, channel).getDestination())) {
              out.add(s);
            }
          }
        }
        if (!out.isEmpty() && !out.equals(p)) { // split happen, exit
          break;
        }
        if (out.equals(p)) { // no split, don't waste time
          out.clear();
        }
      }
      return out;
    }

    private Set<Set<State>> split(Set<State> p, Set<State> splitter) {
      Set<Set<State>> splitP = new HashSet<Set<State>>();
      Set<State> notsplitter = new HashSet<State>(p);

      notsplitter.removeAll(splitter);
      splitP.add(splitter);
      splitP.add(notsplitter);

      return splitP;
    }

  }

  static ConcurrentHashMap<State, Set<Set<State>>> eMap;

  public static void seteMap(ConcurrentHashMap<State, Set<Set<State>>> eMap) {
    ConcurrentSolver.eMap = eMap;
  }

  private TS ts;
  private TS p;
  private Set<String> channels;
  List<Worker> wList;
  final CyclicBarrier roundBarrier;
  final CyclicBarrier updateBarrier;

  private ExecutorService service;

  public ConcurrentSolver(ConcurrentHashMap<State, Set<Set<State>>> indexedFamily, TS t, TS p, Set<String> sch) {
    eMap = new ConcurrentHashMap<>(indexedFamily);
    this.channels = sch;
    this.ts = t;
    this.p = p;
    wList = new ArrayList<Worker>();
    int N = eMap.size() + 1;
    roundBarrier = new CyclicBarrier(N);
    updateBarrier = new CyclicBarrier(N);
    service = Executors.newFixedThreadPool(N);
    int i = 0;
    for (State s : eMap.keySet()) {
      Worker w = new Worker(s.getId(), eMap.get(s), eMap, channels);
      wList.add(i, w);
      i += i;
      service.execute(w);
    }
  }

  public TS run() {
    // int counter = 0;
    boolean fixed = false;
    try {
      while (!fixed) {
        // counter += 1;
        // System.out.println("\n\n SYNCHRONISATION ROUND#" + counter + "\n\n");
        roundBarrier.await();
        fixed = updateMap();
        if (!fixed) {
          updateBarrier.await();
        }
      }
      updateBarrier.reset();
      service.shutdown();

      Set<Set<State>> rho_f = new HashSet<>();
      rho_f = buildFinalRho();

      CompressedTS c = new CompressedTS("s-" + this.ts.getName());
      TS t = c.DoCompress(this.ts, rho_f);
      return t;
    } catch (InterruptedException | BrokenBarrierException e) {
      e.printStackTrace();
      return null;
    }

  }

  private Set<Set<State>> buildFinalRho() {
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
    return rho_intersect;
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
    //if (fixedPoint) {
     // printFixedRho(eMap);
    //}

    for (int i = 0; i < wList.size(); i++) {
      wList.get(i).setlMap(eMap);
    }

    return fixedPoint;
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
    gp.printText();

  }

}
