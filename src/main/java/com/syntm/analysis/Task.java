package com.syntm.analysis;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.syntm.lts.State;
import com.syntm.lts.Trans;

public class Task implements Callable<Set<Set<State>>> {
  State epsilon;
  Set<Set<State>> rhoEpsilon;
  Set<Set<State>> rhoTemp;
  Set<String> channels;
  ConcurrentHashMap<State, Set<Set<State>>> lMap;

  Task(String e, Set<Set<State>> rhoSet, ConcurrentMap<State, Set<Set<State>>> eMap, // O(m + n)
      Set<String> channels) {
    this.channels = new HashSet<>(channels);
    for (State state : eMap.keySet()) { // O(n)
      if (state.getId().equals(e)) {
        this.epsilon = state;
        break;
      }
    }
    this.rhoEpsilon = new HashSet<>(rhoSet);

    this.rhoTemp = new HashSet<>(rhoSet);
    this.lMap = new ConcurrentHashMap<>();
    for (Trans tr_e : epsilon.getTrans()) { // O(m)
      Set<Set<State>> rho = new HashSet<>(eMap.get(tr_e.getDestination()));
      this.lMap.put(tr_e.getDestination(), rho);
    }

  }

  // Complexity O(cmn^4 + cm^2n^3)
  @Override
  public Set<Set<State>> call() throws Exception {
    for (String ch : channels) { // O(cmn^4 + cm^2n^3)
      for (Trans trEpsilon : epsilon.getTrans()) { // O(mn^4 + m^2n^3)
        Set<Set<State>> ePartitions = new HashSet<>(this.lMap.get(trEpsilon.getDestination()));
        for (Set<State> ePrime : ePartitions) { // O(n^4 + mn^3)
          for (Set<State> partition : this.rhoEpsilon) { // O(n^3 + mn^2)
            Set<State> splitter = applyEBisim(partition, trEpsilon, ePrime, ch);
            if (!splitter.isEmpty() && !splitter.equals(partition)) {
              Set<Set<State>> splitP; // = new HashSet<>();
              splitP = split(partition, splitter); // O(n) confirmed
              rhoTemp.remove(partition);
              rhoTemp.addAll(splitP);

            }
          }
        }
      }
    }
    return rhoTemp;
  }

  public State getEpsilon() {
    return epsilon;
  }

  public Set<Set<State>> getRhoEpsilon() {
    return new HashSet<>(rhoEpsilon);
  }

  public void setRhoEpsilon(Set<Set<State>> rhoEpsilon) {
    this.rhoEpsilon = new HashSet<>(rhoEpsilon);
  }

  public Set<Set<State>> getRhoTemp() {
    return new HashSet<>(rhoTemp);
  }

  public void setlMap(ConcurrentMap<State, Set<Set<State>>> lMap) {
    for (Trans tr_e : this.epsilon.getTrans()) {
      Set<Set<State>> rho = new HashSet<>(lMap.get(tr_e.getDestination()));
      this.lMap.put(tr_e.getDestination(), rho);
    }
  }

  /**
   * Complexity O(n^3 + mn^2) but can be combined with an outer
   * <code>for each block in partition</code> to not add another O(n)
   * 
   * @param p
   * @param trEpsilon
   * @param ePrime
   * @param channel
   * @return
   */
  private Set<State> applyEBisim(Set<State> p, Trans trEpsilon, Set<State> ePrime, String channel) {
    Set<State> out = new HashSet<>();

    if (epsilon.enable(epsilon, channel)
        && trEpsilon.getAction().equals(channel)) {
      for (State s : p) { // Does not add any complexity because of the outer loop
        for (State sPrime : p) { // O(n^2 + mn)
          // O(m+n)

          // 3c
          if (s.canExactSilent(s.getOwner(), s, channel)
              && !sPrime.canDirectReaction(sPrime.getOwner(), sPrime, channel)) {
            if (sPrime.canExactSilent(sPrime.getOwner(), sPrime, channel)) {
              if (ePrime.contains(s.takeExactSilent(s.getOwner(), s,
                  channel).getDestination())
                  && ePrime.contains(sPrime.takeExactSilent(sPrime.getOwner(), sPrime,
                      channel).getDestination())) {
                out.add(s);
                out.add(sPrime);
              }
            } else {
              if (!sPrime.getListen().getChannels().contains(channel)) {
                if (ePrime.contains(s.takeExactSilent(s.getOwner(), s,
                    channel).getDestination())
                    && ePrime.contains(sPrime)) {
                  out.add(s);
                  out.add(sPrime);
                }
              }
            }
          }

          // 3b
          if (!s.getListen().getChannels().contains(channel) &&
              !sPrime.getListen().getChannels().contains(channel)) {
            if (ePrime.contains(s) && ePrime.contains(sPrime)) {
              out.add(s);
              out.add(sPrime);
            }
          }

          // O(m+n) below
          // 3d
          if (s.canDirectReaction(s.getOwner(), s, channel) // O(m)
              && !sPrime.canExactSilent(sPrime.getOwner(), sPrime, channel)) {
            if (sPrime.canDirectReaction(sPrime.getOwner(), sPrime, channel)) {
              if (ePrime.contains(s.takeDirectReaction(s.getOwner(), s, channel).getDestination())
                  && ePrime
                      .contains(sPrime.takeDirectReaction(sPrime.getOwner(), sPrime, channel).getDestination())) {
                out.add(s);
                out.add(sPrime);
              }
            } else {
              Set<State> reach = sPrime.weakBFS(sPrime.getOwner(), sPrime, channel); // O(m+n)
              if (reach.size() != 1) {
                for (State sReach : reach) { // O(n)
                  if (sReach.canDirectReaction(sReach.getOwner(), sReach, channel)) {
                    if (ePrime.contains(sReach.takeDirectReaction(sReach.getOwner(), sReach,
                        channel).getDestination())
                        &&
                        ePrime.contains(s.takeDirectReaction(s.getOwner(), s,
                            channel).getDestination())) {
                      out.add(s);
                      out.add(sPrime);
                    }
                  }
                }
              }
            }
          }
        }
      }
      // O(m+n) below here
      // 3a
      if (out.isEmpty() && !epsilon.canTakeInitiative(epsilon.getOwner(), epsilon, channel)
          && epsilon.getListen().getChannels().contains(channel)) {
        for (State s : p) { // Totals O(m+n) with the outer loop
          if (s.canTakeInitiative(s.getOwner(), s, channel)) {
            if (ePrime.contains(s.takeInitiative(s.getOwner(), s, channel).getDestination())) {
              out.add(s);
            }
          }
        }
      }
    }

    // 2
    if (out.isEmpty() && !epsilon.getListen().getChannels().contains(channel)) {
      for (Set<State> partition : rhoEpsilon) { // O(m+n)
        for (State s : p) {
          if (s.canTakeInitiative(s.getOwner(), s, channel)) {
            if (partition.contains(s.takeInitiative(s.getOwner(), s, channel).getDestination())) {
              out.add(s);
            }
          }
        }
        if (!out.isEmpty() && !out.equals(p)) {
          break;
        }
        if (out.equals(p)) {
          out.clear();
        }
      }
    }

    return out;
  }

  private Set<Set<State>> split(Set<State> p, Set<State> splitter) {
    Set<Set<State>> splitP = new HashSet<>();
    Set<State> notsplitter = new HashSet<>(p);

    notsplitter.removeAll(splitter); // O(n) confirmed by the internet
    splitP.add(splitter);
    splitP.add(notsplitter);

    return splitP;
  }

}
