package com.syntm.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.syntm.lts.CompressedTS;
import com.syntm.lts.State;
import com.syntm.lts.TS;
import com.syntm.lts.Trans;

public class SeqSolver {
    private Set<String> channels;
    private HashMap<State, Set<Set<State>>> epsilonMap;
    private TS transSystem;

    /**
     * A sequential version of the parallel algorithm in ESolver.java
     * @param indexedFamily A concurrent hashmap with an initial partition mapped to each state.
     * @param transSystem   The transition system to be reduced.
     * @param channels      A set of the available channels.
     * @return              An object callable with <code>run()</code> to reduce the transition system.
     */
    public SeqSolver(ConcurrentHashMap<State, Set<Set<State>>> indexedFamily, 
                  TS transSystem, Set<String> channels) {
        this.epsilonMap = new HashMap<>(indexedFamily);
        this.transSystem = transSystem;
        this.channels = channels;

    }

    /**
     * The method to call to run the algorithm.
     * @return The reduced transition system.
     */
    public TS run() {
        // -- Code from Task.java adapted to be sequential -- //
        boolean fixedPoint = false;

        while (!fixedPoint) {
            HashMap<State, Set<Set<State>>> rho_results = new HashMap<>();
            
            for (State epsilon : epsilonMap.keySet()) {
                Set<Set<State>> rho_temp = new HashSet<>(epsilonMap.get(epsilon));
                for (String ch : channels) {
                    for (Trans trEpsilon : epsilon.getTrans()) {
                        Set<Set<State>> ePartitions = new HashSet<>(this.epsilonMap.get(trEpsilon.getDestination()));
                        for (Set<State> ePrime : ePartitions) {
                            for (Set<State> partition : this.epsilonMap.get(epsilon)) {
                                Set<State> splitter = applyEBisim(partition, trEpsilon, ePrime, ch, epsilon);
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
                rho_results.put(epsilon, rho_temp);
    
            }

            // update epsilonmap with the new partitions
            fixedPoint = true;
            for (State e : rho_results.keySet()) {
                if (!epsilonMap.get(e).equals(rho_results.get(e))) {
                    // not a fixed point, repeat
                    fixedPoint = false;
                    epsilonMap.put(e, rho_results.get(e));
                }
            }
        }

        Set<Set<State>> rho_final = new HashSet<>();
        for (State s : epsilonMap.keySet()) {
            // Gets the partition from the initial state.
            if (this.transSystem.initStateEqLabel(s)) {
                if (epsilonMap.get(s).size() == 1) return this.transSystem;
                rho_final.addAll(epsilonMap.get(s));
                break;
            } 
        }

        CompressedTS c = new CompressedTS("s-" + this.transSystem.getName());
        return c.compressedTS(this.transSystem, rho_final);
    }

    /**
     * Def 5.4 in the paper
     * @param p         The starting block
     * @param trEpsilon A transition (from p to ePrime)
     * @param ePrime    The block where the transition ends up.
     * @param channel   A channel name
     * @param epsilon   A state (our addition)
     * @return          A possible splitter for the block.
     */
    private Set<State> applyEBisim(Set<State> p, Trans trEpsilon, 
                                   Set<State> ePrime, String channel, 
                                   State epsilon) {
        Set<State> out = new HashSet<State>();
    
        if (epsilon.enable(epsilon, channel) // activating a channel
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
                                            .contains(sPrime.takeDirectReaction(sPrime.getOwner(), sPrime, channel)
                                                    .getDestination())) {
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
                if (!out.isEmpty() && !out.equals(p)) break;
                if (out.equals(p))                    out.clear();
            }
        }

        return out;
    }
    
    /**
     * A splitter function
     * @param p         The block to split
     * @param splitter  The block to split with
     * @return          The resulting blocks
     */
    private Set<Set<State>> split(Set<State> p, Set<State> splitter) {
        Set<Set<State>> splitP = new HashSet<Set<State>>();
        Set<State> notsplitter = new HashSet<State>(p);

        notsplitter.removeAll(splitter);
        splitP.add(splitter);
        splitP.add(notsplitter);

        return splitP;
    }
}
