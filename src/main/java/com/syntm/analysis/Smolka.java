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
     * @return              An object callable with <code>run()</code> to reduce the transition system using the Kannelakis-Smolka algorithm.
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
        /*
        Queue<Set<State>> pi_waiting = new LinkedList<>();
        Set<Set<State>> pi_blocks = new HashSet<>();
        while (pi_waiting.size() > 0) {
            Set<State> block = pi_waiting.remove();
            for (State epsilon : epsilonMap.keySet()) {
              for (String channel : this.channels) {
                for (Trans trEpsilon : epsilon.getTrans()) { // For every state (which is epsilon)
                    Set<Set<State>> ePartitions = new HashSet<>(this.epsilonMap.get(trEpsilon.getDestination()));
                    for (Set<State> ePrime : ePartitions) {
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
        }*/

        // π := {Pr}
        // changed := true
        // while changed do
        //   changed := false
        //   for each block B ∈ π do
        //     for each action a do
        //       sort the a-labelled transitions from states in B
        //       if split(B, a, π) = {B1, B2} =/= {B}
        //         then refine π by replacing B with B1 and B2, and set changed
        //              to true

        System.out.println("Starting the thing!");
        Set<Set<State>> x = epsilonMap.values().iterator().next();
        System.out.println(x);
        Queue<Set<State>> pi_waiting = new LinkedList<>(x);
        Set<Set<State>> pi_blocks = new HashSet<>();
        while (pi_waiting.size() > 0) {
          Set<State> block = pi_waiting.remove();
          for (String channel : this.channels) {
            Set<Set<State>> splitP = smolkaSplit(block, pi_blocks, channel);
            System.out.println(splitP);
            if (splitP.size() > 1) {
              pi_waiting.addAll(splitP);
              pi_blocks.remove(block);
              pi_blocks.addAll(splitP);
            }
          }
        }
              
        // Set<Set<State>> rho_final = new HashSet<>();
        // for (State s : epsilonMap.keySet()) {
        //     // What actually happens here?
        //     if (this.transSystem.initStateEqLabel(s)) {
        //         if (epsilonMap.get(s).size() == 1) return this.transSystem;
        //         rho_final.addAll(epsilonMap.get(s));
        //         break;
        //     } 
        // }

        System.out.println("Size of pi_blocks: " + pi_blocks.size());
        System.out.println(pi_blocks);

        CompressedTS c = new CompressedTS("s-" + this.transSystem.getName());
        // return c.compressedTS(this.transSystem, rho_final);
        return c.compressedTS(this.transSystem, pi_blocks);
    }

    // function split(B, a, π)
    // choose some state s ∈ B
    // B1, B2 := ∅
    // for each state t ∈ B do
    //   if s and t can reach the same set of blocks in π via a-labelled transitions
    //     then B1 := B1 ∪ {t}
    //   else B2 := B2 ∪ {t}
    //     if B2 is empty then return {B1}
    //     else return {B1, B2}
    
    /**
     * The split function used by the Kanellakis-Smolka algorithm.
     * @param block   The splitter block
     * @param pi      The partition
     * @param channel The channel/action to split on
     * @return        The split block(s)
     */
    private Set<Set<State>> smolkaSplit(Set<State> block, Set<Set<State>> pi, String channel) {
      Set<Set<State>> output = new HashSet<>();
      Set<State> block1 = new HashSet<>();
      Set<State> block2 = new HashSet<>();
      State s = block.iterator().next();
      
      Set<Set<State>> sBlocks = new HashSet<>();
      for (Trans t : s.getTrans()) {
        if (t.getAction() == channel) {
          for (Set<State> b : pi) {
            if (b.contains(t.destination)) {
              // Add the block in the partition that contains the destination state.
              sBlocks.add(b);
              break;
            }
          }
        }
      }

      for (State t: block) {
        //Check transfer
        Set<Set<State>> blockDestinationPartitions = new HashSet<>();
        Set<Trans> transitions = t.getTrans();
        for (Trans trans : transitions) {
          if (trans.getAction() == channel) {
            for (Set<State> b : pi) {
              if (b.contains(trans.destination)) {
                // Add the block in the partition that contains the destination state.
                blockDestinationPartitions.add(b);
                break;
              }
            }
          }
        }
        
        // if s and t can reach the same set of blocks in π via a-labelled transitions
        if (blockDestinationPartitions.equals(sBlocks)) {
          block1.add(t);
        } else {
          block2.add(t);
        }
      }

      if (block2.isEmpty()) {
        output.add(block2);
        return output;
      }
      else{
        output.add(block1);
        output.add(block2);
        return output;
      }
    }

    /**
     * We have no idea what happens here
     * @param p         a block?
     * @param trEpsilon A transition (from where to where?)
     * @param ePrime    Another block?
     * @param channel   A channel name
     * @param epsilon   A state
     * @return          A block?
     */
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
