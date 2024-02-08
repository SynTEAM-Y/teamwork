package com.syntm.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.syntm.lts.CompressedTS;
import com.syntm.lts.State;
import com.syntm.lts.TS;
import com.syntm.lts.Trans;

/**
 * An object for reducing a transition system using the Kanellakis-Smolka algorithm.
 */
public class Smolka {
    private Set<String> channels;
    private HashMap<State, Set<Set<State>>> epsilonMap;
    private TS transSystem;

    /**
     * Contructor for a class using the Kanellakis-Smolka algorithm to reduce the transition system.
     * @param indexedFamily A concurrent hashmap with an initial partition mapped to each state.
     * @param transSystem   The transition system to be reduced.
     * @param channels      A set of the available channels.
     * @return              An object callable with `run()` to reduce the transition system using the Kannelakis-Smolka algorithm.
     */
    public Smolka(ConcurrentHashMap<State, Set<Set<State>>> indexedFamily, 
                  TS transSystem, Set<String> channels) {
        this.epsilonMap = new HashMap<>(indexedFamily);
        this.transSystem = transSystem;
        this.channels = channels;

    }

    /**
     * The method to call to run the Kanellakis-Smolka algorithm.
     * @return The reduced transition system.
     */
    public TS run() {
        
        Queue<Set<State>> pi_waiting = new LinkedList<>();
        Set<Set<State>> pi_blocks = new HashSet<>();
        while (pi_waiting.size() > 0) {
            Set<State> block = pi_waiting.remove();

            for (State epsilon : epsilonMap.keySet()) {
                for (Trans trEpsilon : epsilon.getTrans()) { // For every state (which is epsilon)
                    Set<Set<State>> ePartitions = new HashSet<>(this.epsilonMap.get(trEpsilon.getDestination()));
                    for (Set<State> ePrime : ePartitions) {

                // for (Set<State> block : pi_waiting) {
                        for (String channel : this.channels) {
                            Set<State> splitter = applyEBisim(block, trEpsilon, ePrime, channel, epsilon);
                            if (!splitter.isEmpty() && !splitter.equals(block)) {
                                Set<Set<State>> splitP = split(block, splitter);
                                // pi_waiting.remove(block);
                                pi_waiting.addAll(splitP);
                                pi_blocks.remove(block);
                                pi_blocks.addAll(splitP);
                            }
                            
                        }
                    }
                }
            }

            // }
        }

        // -- Code from Task.java -- //
        // for (State epsilon : epsilonMap.keySet()) {
        //     for (String ch : channels) {
        //         for (Trans trEpsilon : epsilon.getTrans()) {
        //             Set<Set<State>> ePartitions = new HashSet<>(this.lMap.get(trEpsilon.getDestination()));
        //             for (Set<State> ePrime : ePartitions) {
        //                 for (Set<State> partition : this.rho_epsilon) {
        //                     Set<State> splitter = applyEBisim(partition, trEpsilon, ePrime, ch);
        //                     if (!splitter.isEmpty() && !splitter.equals(partition)) {
        //                         Set<Set<State>> splitP = new HashSet<>();
        //                         splitP = split(partition, splitter);
        //                         rho_temp.remove(partition);
        //                         rho_temp.addAll(splitP);
        //                     }
        //                 }
        //             }
        //         }
        //     }
        // }

        Set<Set<State>> rho_final = new HashSet<>();
        for (State s : epsilonMap.keySet()) {
            // What actually happens here?
            if (this.transSystem.initStateEqLabel(s)) {
                if (epsilonMap.get(s).size() == 1) return this.transSystem;
                rho_final.addAll(epsilonMap.get(s));
                break;
            } 
        }

        CompressedTS c = new CompressedTS("s-" + this.transSystem.getName());
        return c.compressedTS(this.transSystem, rho_final);
    }

    private Set<State> applyEBisim(Set<State> p, Trans trEpsilon, 
                                   Set<State> ePrime, String channel, 
                                   State epsilon) {
        Set<State> out = new HashSet<State>();
        //State epsilon; // <-----
    
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
          for (Set<State> partition : epsilonMap.get(epsilon)) {
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
