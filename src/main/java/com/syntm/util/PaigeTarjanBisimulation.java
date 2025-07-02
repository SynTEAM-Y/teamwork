package com.syntm.util;

import java.util.*;

public class PaigeTarjanBisimulation {

    static class Transition {
        int source;
        String action;
        int target;
        Transition(int source, String action, int target) {
            this.source = source;
            this.action = action;
            this.target = target;
        }
    }

    static class Pair<F, S> {
        final F first;
        final S second;
        Pair(F f, S s) {
            this.first = f;
            this.second = s;
        }
    }

    static class RefinablePartition {
        int[] position;   // position[i] = index of state i in states array
        int[] blockID;    // blockID[i] = block number that state i belongs to
        int[] states;     // states array holding all states, permuted during refinement
        int[] blockStart; // start index of each block in states array
        int[] blockEnd;   // end index (exclusive) of each block in states array
        int numBlocks;
        int n;

        RefinablePartition(int n) {
            this.n = n;
            states = new int[n];
            position = new int[n];
            blockID = new int[n];
            // Allocate double size to hold new blocks after splits
            blockStart = new int[2 * n];
            blockEnd = new int[2 * n];

            for (int i = 0; i < n; i++) {
                states[i] = i;
                position[i] = i;
                blockID[i] = 0;
            }
            blockStart[0] = 0;
            blockEnd[0] = n;
            numBlocks = 1;
        } // O(n)

        List<Set<Integer>> getBlocks() {
            List<Set<Integer>> res = new ArrayList<>();
            for (int b = 0; b < numBlocks; b++) {
                Set<Integer> s = new HashSet<>();
                for (int i = blockStart[b]; i < blockEnd[b]; i++) {
                    s.add(states[i]);
                }
                res.add(s);
            }
            return res;
        }

        Set<Integer> getBlockStates(int block) {
            Set<Integer> res = new HashSet<>();
            for (int i = blockStart[block]; i < blockEnd[block]; i++) {
                res.add(states[i]);
            }
            return res;
        }

        /**
         * Refine the partition by splitting blocks according to states in 'mark'.
         * Returns list of new blocks created by splitting.
         */
        List<Integer> refine(Set<Integer> mark) {
            int[] markCount = new int[numBlocks];
            for (int s : mark) {
                markCount[blockID[s]]++;
            }

            List<Integer> newBlocks = new ArrayList<>();

            int currentNumBlocks = numBlocks; // avoid modifying during iteration

            for (int b = 0; b < currentNumBlocks; b++) {
                int start = blockStart[b], end = blockEnd[b];
                int size = end - start;
                if (markCount[b] == 0 || markCount[b] == size) continue;

                // Partition states in block b into those in mark and not in mark
                int i = start, j = end - 1;
                while (i <= j) {
                    while (i <= j && mark.contains(states[i])) i++;
                    while (i <= j && !mark.contains(states[j])) j--;
                    if (i < j) {
                        int a = states[i], b_ = states[j];
                        states[i] = b_;
                        states[j] = a;
                        position[a] = j;
                        position[b_] = i;
                        i++;
                        j--;
                    }
                }

                int mid = i;
                int oldStart = blockStart[b], oldEnd = blockEnd[b];
                int newBlock = numBlocks++;
                blockStart[newBlock] = oldStart;
                blockEnd[newBlock] = mid;
                blockStart[b] = mid;
                blockEnd[b] = oldEnd;

                // Update blockID for moved states
                for (int k = blockStart[newBlock]; k < blockEnd[newBlock]; k++) {
                    blockID[states[k]] = newBlock;
                }

                newBlocks.add(newBlock);
            }

            return newBlocks;
        }
    }

    /**
     * Compute the strong bisimulation partition of a labeled transition system (LTS)
     * using Paige-Tarjan algorithm.
     *
     * @param n Number of states
     * @param transitions List of transitions (source, action, target)
     * @param actions Set of action labels
     * @return List of sets of states representing bisimulation equivalence classes
     */
    public static List<Set<Integer>> computeBisimulation(int n, List<Transition> transitions, Set<String> actions) {
        // Build reverse transition map: action -> target -> sources
        Map<String, Map<Integer, Set<Integer>>> reverse = new HashMap<>();
        for (String a : actions) reverse.put(a, new HashMap<>());
        for (Transition t : transitions) {
            reverse.get(t.action).computeIfAbsent(t.target, k -> new HashSet<>()).add(t.source);
        } // O(m)

        RefinablePartition rp = new RefinablePartition(n);
        Deque<Pair<Integer, String>> worklist = new ArrayDeque<>();

        // Initialize worklist with all actions for the initial block 0
        for (String a : actions) {
            worklist.add(new Pair<>(0, a));
        } // O(k)

        while (!worklist.isEmpty()) {
            Pair<Integer, String> entry = worklist.poll();
            int block = entry.first;
            String action = entry.second;
            Map<Integer, Set<Integer>> preMap = reverse.get(action);

            Set<Integer> B = rp.getBlockStates(block);
            Set<Integer> mark = new HashSet<>();
            for (int b : B) {
                if (preMap.containsKey(b)) {
                    mark.addAll(preMap.get(b));
                }
            }

            if (!mark.isEmpty()) {
                List<Integer> newBlocks = rp.refine(mark);
                for (int b : newBlocks) {
                    for (String a : actions) {
                        worklist.add(new Pair<>(b, a));
                    }
                }
            }
        }

        return rp.getBlocks();
    }

    public static void main(String[] args) {
        List<Transition> transitions = List.of(
            new Transition(0, "a", 1),
            new Transition(0, "b", 2),
            new Transition(1, "d", 4),
            new Transition(1, "c", 3),
            new Transition(5, "a", 6),
            new Transition(5, "b", 7),
            new Transition(6, "d", 9),
            new Transition(7, "c", 8)
        );
        Set<String> actions = Set.of("a", "b", "c", "d");
        int numStates = 10;

        List<Set<Integer>> result = computeBisimulation(numStates, transitions, actions);
        System.out.println("Strong bisimulation equivalence classes:");
        for (Set<Integer> block : result) {
            System.out.println(block);
        }
    }
}
