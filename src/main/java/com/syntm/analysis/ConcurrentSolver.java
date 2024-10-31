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

import com.syntm.lts.CompressedTS;
import com.syntm.lts.State;
import com.syntm.lts.TS;
import com.syntm.lts.Trans;

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
  private Set<String> channels;
  List<Worker> wList;
  final CyclicBarrier roundBarrier;
  final CyclicBarrier updateBarrier;

  private ExecutorService service;

  public ConcurrentSolver(ConcurrentHashMap<State, Set<Set<State>>> indexedFamily, TS t, Set<String> sch) {
    eMap = new ConcurrentHashMap<>(indexedFamily);
    this.channels = sch;
    this.ts = t;
    wList = new ArrayList<Worker>();
    int N = eMap.size() + 1;
    roundBarrier = new CyclicBarrier(N);
    updateBarrier = new CyclicBarrier(N);
    // int corecount = Runtime.getRuntime().availableProcessors();
    service = Executors.newFixedThreadPool(N);
    int i = 0;
    for (State s : eMap.keySet()) {
      Worker w = new Worker(s.getId(), eMap.get(s), eMap, channels);
      wList.add(i, w);
      i += i;
      service.execute(w);
      // new Thread(w).start();
    }
  }

  public TS run() {
    //int counter = 0;
    boolean fixed = false;
    try {
      while (!fixed) {
        //counter += 1;
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
      String id = ts.getInitState().getId();
      for (State s : eMap.keySet()) {
        if (s.getId().equals(id)) {
          rho_f = new HashSet<>(eMap.get(s));
        }
      }

      CompressedTS c = new CompressedTS("s-" + this.ts.getName());
      TS t = c.DoCompress(this.ts, rho_f);
      return t;
    } catch (InterruptedException | BrokenBarrierException e) {
      e.printStackTrace();
      return null;
    }

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
    System.out.println("Fixed map -> " + eMap);
    }

    for (int i = 0; i < wList.size(); i++) {
      wList.get(i).setlMap(eMap);
    }

    return fixedPoint;
  }
}
