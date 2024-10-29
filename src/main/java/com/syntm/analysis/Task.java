package com.syntm.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import com.syntm.lts.State;
import com.syntm.lts.Trans;

public class Task implements Callable<Set<Set<State>>> {
  State epsilon;
  Set<Set<State>> rho_epsilon;
  Set<Set<State>> rho_temp;
  Set<String> channels;
  ConcurrentHashMap<State, Set<Set<State>>> lMap;

  Task(String e, Set<Set<State>> rho_Set, ConcurrentHashMap<State, Set<Set<State>>> eMap,
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

  @Override
  public Set<Set<State>> call() throws Exception {
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
    return rho_temp;

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

  // private Set<State> applyEBisim(Set<State> p, Trans trEpsilon, Set<State>
  // ePrime, String channel) {
  // Set<State> out = new HashSet<State>();
  // // 2. b, c, a with parameter invovled
  // if (epsilon.enable(epsilon, channel)
  // && trEpsilon.getAction().equals(channel)) {

  // if (epsilon.getOwner().getInterface().getChannels().contains(channel)) {

  // // Silent moves
  // out = sSilentToE(p, ePrime, out, channel);

  // // reaction moves
  // if (out.isEmpty()) {
  // out = sReactToE(p, ePrime, out, channel);
  // }
  // }
  // // initiation by s
  // if (out.isEmpty())
  // out = sInitiateToE(p, ePrime, out, channel);

  // }

  // // 2 b, c, a without involving the parameter
  // if (out.isEmpty() && !epsilon.getListen().getChannels().contains(channel)) {
  // // Epsilon does not participate
  // out = sInitiateAlone(p, out, channel);

  // }

  // return out;
  // }

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

  private Set<State> sReactToE(Set<State> p, Set<State> ePrime, Set<State> out, String channel) {
    for (State s : p) {
      if (s.canDirectReaction(s.getOwner(), s, channel)) {
        if (ePrime.contains(s.takeDirectReaction(s.getOwner(), s, channel).getDestination())) {
          out.add(s);
        }
      }
    }
    Set<State> pPrime = new HashSet<>();
    pPrime.addAll(p);
    pPrime.removeAll(out);
    if (!out.isEmpty()) {
      for (State s : pPrime) {
        if (!s.canDirectReaction(s.getOwner(), s, channel)
            && !s.canExactSilent(s.getOwner(), s, channel)) {
          Set<State> reach = s.weakBFS(s.getOwner(), s, channel);
          if (reach.size() != 1) {
            for (State sReach : reach) {
              if (sReach.canDirectReaction(sReach.getOwner(), sReach, channel)) {
                if (ePrime
                    .contains(sReach.takeDirectReaction(sReach.getOwner(), sReach,
                        channel).getDestination())) {
                  out.add(s);

                }
              }
            }
          }
        }
      }
    }

    return out;
  }

  private Set<State> sSilentToE(Set<State> p, Set<State> ePrime, Set<State> out, String channel) {
    boolean flag = false;
    for (State s : p) {
      // 3.c
      if (s.canExactSilent(s.getOwner(), s, channel)) {
        if (ePrime.contains(s.takeExactSilent(s.getOwner(), s,
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

        if (!s.canExactSilent(s.getOwner(), s, channel)
            && !s.canDirectReaction(s.getOwner(), s, channel)) {

          //
          Set<State> reach = s.weakBFS(s.getOwner(), s, channel);
          if (reach.size() != 1) {
            for (State sReach : reach) {
              if (sReach.canDirectReaction(sReach.getOwner(), sReach, channel)) {
                flag = true;
              }
              if (sReach.canExactSilent(sReach.getOwner(), sReach, channel)) {
                flag = true;
                if (!sReach.canDirectReaction(sReach.getOwner(), sReach, channel)) {
                  if (ePrime
                      .contains(sReach.takeExactSilent(sReach.getOwner(), sReach,
                          channel).getDestination())) {
                    out.add(s);

                  }
                }
              }
            }
          }
          //
          if (!flag && !s.getListen().getChannels().contains(channel)) {
            if (ePrime.contains(s)) {
              out.add(s);
            }
          }
        }
        flag = false;
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
