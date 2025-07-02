package com.syntm.analysis;
/*
Author:  Yehia Abd Alrahman (yehiaa@chalmers.se)
Task.java (c) 2024
Desc: Rho Epsilon computation for ESolver
Created:  17/11/2024 09:45:55
Updated:  23/06/2025 01:05:27
Version:  1.1
*/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    this.epsilon = eMap.keySet()
        .stream()
        .filter(s -> s.getId().equals(e))
        .collect(Collectors.toSet()).iterator().next();
    // for (State state : eMap.keySet()) {
    // if (state.getId().equals(e)) {
    // this.epsilon = state;
    // break;
    // }
    // }
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
        List<Set<State>> ePartitions = new ArrayList<Set<State>>(this.lMap.get(trEpsilon.getDestination()));
        ePartitions.sort((e1, e2) -> e2.size() - e1.size());
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

  private Set<State> applyEBisim(Set<State> p, Trans trEpsilon, Set<State> ePrime, String channel) {
    Set<State> out = new HashSet<State>();
    // 2. b, c, a with parameter invovled
    if (epsilon.enable(epsilon, channel)
        && trEpsilon.getAction().equals(channel)) {
      // reaction from s
      if (epsilon.getOwner().getInterface().getChannels().contains(channel)) {
        out = sRacToE(p, ePrime, out, channel);
      }
      // initiation by s
      if (out.isEmpty())
        out = sInitiateToE(p, ePrime, out, channel);

    }

    // 2 b, c, a without involving the parameter
    if (out.isEmpty() && !epsilon.getListen().getChannels().contains(channel)) {
      // Epsilon does not participate
      out = sInitiateAlone(p, out, channel);

    }

    return out;
  }

  private Set<State> sRacToE(Set<State> p, Set<State> ePrime, Set<State> out, String channel) {
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
            // if (ePrime.contains(s))
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

// package com.syntm.analysis;
// /*
// Author:  Yehia Abd Alrahman (yehiaa@chalmers.se)
// Task.java (c) 2024
// Desc: Rho Epsilon computation for ESolver with BitSet optimization internally
// Created:  17/11/2024 09:45:55
// Updated:  23/06/2025
// Version:  1.3
// */

// import java.util.ArrayList;
// import java.util.BitSet;
// import java.util.HashMap;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Set;
// import java.util.concurrent.Callable;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.stream.Collectors;

// import com.syntm.lts.State;
// import com.syntm.lts.Trans;

// public class Task implements Callable<Set<Set<State>>> {

//   public interface StateIndexer {
//     int size();
//     int toIndex(State s);
//     State fromIndex(int i);
//     BitSet toBitSet(Set<State> states);
//     Set<State> fromBitSet(BitSet bs);
//   }

//   private final StateIndexer indexer;

//   State epsilon;
//   Set<Set<State>> rho_epsilon;
//   Set<Set<State>> rho_temp;
//   Set<String> channels;
//   ConcurrentHashMap<State, Set<Set<State>>> lMap;

//   // Internal BitSet versions of rho_epsilon and rho_temp for performance
//   List<BitSet> rho_epsilon_bs;
//   List<BitSet> rho_temp_bs;

//   public Task(String e, Set<Set<State>> rho_Set, ConcurrentHashMap<State, Set<Set<State>>> eMap,
//       Set<String> channels, StateIndexer indexer) {
//     this.indexer = indexer;
//     this.channels = new HashSet<>(channels);

//     this.epsilon = eMap.keySet()
//         .stream()
//         .filter(s -> s.getId().equals(e))
//         .findFirst()
//         .orElseThrow(() -> new IllegalArgumentException("State id not found: " + e));

//     this.rho_epsilon = new HashSet<>(rho_Set);
//     this.rho_temp = new HashSet<>(rho_Set);
//     this.lMap = new ConcurrentHashMap<>();
//     for (Trans tr_e : epsilon.getTrans()) {
//       Set<Set<State>> rho = new HashSet<>(eMap.get(tr_e.getDestination()));
//       this.lMap.put(tr_e.getDestination(), rho);
//     }

//     // Build BitSet lists for rho_epsilon and rho_temp
//     this.rho_epsilon_bs = setsToBitSets(rho_epsilon);
//     this.rho_temp_bs = setsToBitSets(rho_temp);
//   }

//   private List<BitSet> setsToBitSets(Set<Set<State>> setOfSets) {
//     List<BitSet> list = new ArrayList<>();
//     for (Set<State> s : setOfSets) {
//       list.add(indexer.toBitSet(s));
//     }
//     return list;
//   }

//   private Set<Set<State>> bitSetsToSets(List<BitSet> bitSetList) {
//     Set<Set<State>> result = new HashSet<>();
//     for (BitSet bs : bitSetList) {
//       result.add(indexer.fromBitSet(bs));
//     }
//     return result;
//   }

//   @Override
//   public Set<Set<State>> call() throws Exception {
//     for (String ch : channels) {
//       for (Trans trEpsilon : epsilon.getTrans()) {
//         // Convert lMap sets to BitSet lists on demand
//         List<BitSet> ePartitions_bs = lMap.get(trEpsilon.getDestination()).stream()
//             .map(indexer::toBitSet)
//             .sorted((a, b) -> b.cardinality() - a.cardinality())
//             .collect(Collectors.toList());

//         List<BitSet> newRhoTemp = new ArrayList<>(rho_temp_bs);

//         for (BitSet ePrime_bs : ePartitions_bs) {
//           HashMap<BitSet, BitSet> splitters = new HashMap<>();
//           for (BitSet partition_bs : newRhoTemp) {
//             if (partition_bs.cardinality() > 1) {
//               BitSet splitter_bs = applyEBisim(partition_bs, trEpsilon, ePrime_bs, ch);
//               if (!splitter_bs.isEmpty() && !splitter_bs.equals(partition_bs)) {
//                 splitters.put(partition_bs, splitter_bs);
//               }
//             }
//           }

//           if (!splitters.isEmpty()) {
//             for (BitSet p_bs : splitters.keySet()) {
//               List<BitSet> splitP_bs = split(p_bs, splitters.get(p_bs));
//               newRhoTemp.remove(p_bs);
//               newRhoTemp.addAll(splitP_bs);
//             }
//           }
//         }
//         rho_temp_bs = newRhoTemp;
//       }
//     }

//     // Update rho_temp Set<Set<State>> to match BitSet version before returning
//     rho_temp = bitSetsToSets(rho_temp_bs);
//     return new HashSet<>(rho_temp);
//   }

//   public State getEpsilon() {
//     return epsilon;
//   }

//   public Set<Set<State>> getRho_epsilon() {
//     return new HashSet<>(rho_epsilon);
//   }

//   public void setRho_epsilon(Set<Set<State>> rho_epsilon) {
//     this.rho_epsilon = new HashSet<>(rho_epsilon);
//     this.rho_epsilon_bs = setsToBitSets(this.rho_epsilon);
//   }

//   public Set<Set<State>> getRho_temp() {
//     return new HashSet<>(rho_temp);
//   }

//   public void setlMap(ConcurrentHashMap<State, Set<Set<State>>> lMap) {
//     this.lMap.clear();
//     for (Trans tr_e : this.epsilon.getTrans()) {
//       Set<Set<State>> rho = new HashSet<>(lMap.get(tr_e.getDestination()));
//       this.lMap.put(tr_e.getDestination(), rho);
//     }
//   }

//   // --- BitSet-based applyEBisim and helper methods ---

//   private BitSet applyEBisim(BitSet p_bs, Trans trEpsilon, BitSet ePrime_bs, String channel) {
//     BitSet out_bs = new BitSet(indexer.size());

//     // Condition: epsilon enables on channel and trEpsilon action equals channel
//     if (epsilon.enable(epsilon, channel) && trEpsilon.getAction().equals(channel)) {
//       if (epsilon.getOwner().getInterface().getChannels().contains(channel)) {
//         out_bs = sRacToE(p_bs, ePrime_bs, out_bs, channel);
//       }
//       if (out_bs.isEmpty()) {
//         out_bs = sInitiateToE(p_bs, ePrime_bs, out_bs, channel);
//       }
//     }

//     if (out_bs.isEmpty() && !epsilon.getListen().getChannels().contains(channel)) {
//       out_bs = sInitiateAlone(p_bs, out_bs, channel);
//     }
//     return out_bs;
//   }

//   private BitSet sRacToE(BitSet p_bs, BitSet ePrime_bs, BitSet out_bs, String channel) {
//     // For each state in p_bs
//     for (int i = p_bs.nextSetBit(0); i >= 0; i = p_bs.nextSetBit(i + 1)) {
//       State s = indexer.fromIndex(i);
//       if (s.canAnyReaction(s.getOwner(), s, channel)) {
//         State dest = s.takeAnyReaction(s.getOwner(), s, channel).getDestination();
//         int destIdx = indexer.toIndex(dest);
//         if (ePrime_bs.get(destIdx)) {
//           out_bs.set(i);
//         }
//       }
//     }

//     BitSet pPrime_bs = (BitSet) p_bs.clone();
//     pPrime_bs.andNot(out_bs);

//     if (!out_bs.isEmpty()) {
//       for (int i = pPrime_bs.nextSetBit(0); i >= 0; i = pPrime_bs.nextSetBit(i + 1)) {
//         State s = indexer.fromIndex(i);
//         if (!s.canAnyReaction(s.getOwner(), s, channel)) {
//           if (!s.getListen().getChannels().contains(channel)) {
//             out_bs.set(i);
//           }
//         }
//       }
//     }
//     return out_bs;
//   }

//   private BitSet sInitiateToE(BitSet p_bs, BitSet ePrime_bs, BitSet out_bs, String channel) {
//     if (!epsilon.canTakeInitiative(epsilon.getOwner(), epsilon, channel)
//         && epsilon.getListen().getChannels().contains(channel)) {
//       for (int i = p_bs.nextSetBit(0); i >= 0; i = p_bs.nextSetBit(i + 1)) {
//         State s = indexer.fromIndex(i);
//         if (s.canTakeInitiative(s.getOwner(), s, channel)) {
//           State dest = s.takeInitiative(s.getOwner(), s, channel).getDestination();
//           int destIdx = indexer.toIndex(dest);
//           if (ePrime_bs.get(destIdx)) {
//             out_bs.set(i);
//           }
//         }
//       }
//     }
//     return out_bs;
//   }

//   private BitSet sInitiateAlone(BitSet p_bs, BitSet out_bs, String channel) {
//     outer:
//     for (BitSet partition_bs : rho_epsilon_bs) {
//       for (int i = p_bs.nextSetBit(0); i >= 0; i = p_bs.nextSetBit(i + 1)) {
//         State s = indexer.fromIndex(i);
//         if (s.canTakeInitiative(s.getOwner(), s, channel)) {
//           State dest = s.takeInitiative(s.getOwner(), s, channel).getDestination();
//           int destIdx = indexer.toIndex(dest);
//           if (partition_bs.get(destIdx)) {
//             out_bs.set(i);
//           }
//         }
//       }
//       if (!out_bs.isEmpty() && !out_bs.equals(p_bs)) {
//         break outer;
//       }
//       if (out_bs.equals(p_bs)) {
//         out_bs.clear();
//       }
//     }
//     return out_bs;
//   }

//   private List<BitSet> split(BitSet p_bs, BitSet splitter_bs) {
//     BitSet notsplitter_bs = (BitSet) p_bs.clone();
//     notsplitter_bs.andNot(splitter_bs);

//     List<BitSet> splitP = new ArrayList<>();
//     splitP.add(splitter_bs);
//     splitP.add(notsplitter_bs);

//     return splitP;
//   }
// }
