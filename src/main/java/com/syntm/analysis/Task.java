package com.syntm.analysis;

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
          for (Set<State> partition : this.rho_epsilon) {
            Set<State> splitter = applyEBisim(partition, trEpsilon, ePrime, ch);
            if (!splitter.isEmpty() && !splitter.equals(partition)) {
              Set<Set<State>> splitP = new HashSet<>();
              splitP = split(partition, splitter);
              rho_temp.remove(partition);
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

  private Set<State> applyEBisim(Set<State> p, Trans trEpsilon, Set<State> ePrime, String channel) {
    Set<State> out = new HashSet<State>();

    if (epsilon.enable(epsilon, channel)
        && trEpsilon.getAction().equals(channel)) {
      for (State s : p) {
        for (State sPrime : p) {

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

          if (!s.getListen().getChannels().contains(channel) &&
              !sPrime.getListen().getChannels().contains(channel)) {
            if (ePrime.contains(s) && ePrime.contains(sPrime)) {
              out.add(s);
              out.add(sPrime);
            }
          }

          if (s.canDirectReaction(s.getOwner(), s, channel)
              && !sPrime.canExactSilent(sPrime.getOwner(), sPrime, channel)) {
            if (sPrime.canDirectReaction(sPrime.getOwner(), sPrime, channel)) {
              if (ePrime.contains(s.takeDirectReaction(s.getOwner(), s, channel).getDestination())
                  && ePrime
                      .contains(sPrime.takeDirectReaction(sPrime.getOwner(), sPrime, channel).getDestination())) {
                out.add(s);
                out.add(sPrime);
              }
            } else {
              Set<State> reach = sPrime.weakBFS(sPrime.getOwner(), sPrime, channel);
              if (reach.size() != 1) {
                for (State sReach : reach) {
                  if (sReach.canDirectReaction(sReach.getOwner(), sReach, channel)) {
                    if (ePrime
                        .contains(sReach.takeDirectReaction(sReach.getOwner(), sReach,
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
    }

    if (out.isEmpty() && !epsilon.canTakeInitiative(epsilon.getOwner(), epsilon, channel)
        && epsilon.getListen().getChannels().contains(channel)) {
      for (State s : p) {
        if (s.canTakeInitiative(s.getOwner(), s, channel)) {
          if (ePrime.contains(s.takeInitiative(s.getOwner(), s, channel).getDestination())) {
            out.add(s);
          }
        }
      }
    }

    if (out.isEmpty() && !epsilon.getListen().getChannels().contains(channel)) {
      for (Set<State> partition : rho_epsilon) {
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
    Set<Set<State>> splitP = new HashSet<Set<State>>();
    Set<State> notsplitter = new HashSet<State>(p);

    notsplitter.removeAll(splitter);
    splitP.add(splitter);
    splitP.add(notsplitter);

    return splitP;
  }

}
