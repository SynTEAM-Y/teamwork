package com.syntm.util;

import java.util.*;

public class BalancedPairAssignment {

    static class Edge {
        int to, rev;
        int cap;

        Edge(int to, int rev, int cap) {
            this.to = to;
            this.rev = rev;
            this.cap = cap;
        }
    }

    static class Dinic {
        int N;
        ArrayList<Edge>[] graph;
        int[] level;
        int[] iter;

        @SuppressWarnings("unchecked")
        Dinic(int N) {
            this.N = N;
            graph = new ArrayList[N];
            for (int i = 0; i < N; i++) graph[i] = new ArrayList<>();
            level = new int[N];
            iter = new int[N];
        }

        void addEdge(int u, int v, int cap) {
            graph[u].add(new Edge(v, graph[v].size(), cap));
            graph[v].add(new Edge(u, graph[u].size() - 1, 0));
        }

        boolean bfs(int s, int t) {
            Arrays.fill(level, -1);
            Queue<Integer> queue = new LinkedList<>();
            level[s] = 0;
            queue.offer(s);
            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (Edge e : graph[u]) {
                    if (e.cap > 0 && level[e.to] < 0) {
                        level[e.to] = level[u] + 1;
                        queue.offer(e.to);
                    }
                }
            }
            return level[t] >= 0;
        }

        int dfs(int u, int t, int flow) {
            if (u == t) return flow;
            for (; iter[u] < graph[u].size(); iter[u]++) {
                Edge e = graph[u].get(iter[u]);
                if (e.cap > 0 && level[u] < level[e.to]) {
                    int d = dfs(e.to, t, Math.min(flow, e.cap));
                    if (d > 0) {
                        e.cap -= d;
                        graph[e.to].get(e.rev).cap += d;
                        return d;
                    }
                }
            }
            return 0;
        }

        int maxFlow(int s, int t) {
            int flow = 0;
            while (bfs(s, t)) {
                Arrays.fill(iter, 0);
                int f;
                while ((f = dfs(s, t, Integer.MAX_VALUE)) > 0) {
                    flow += f;
                }
            }
            return flow;
        }
    }

    // Build and test assignment with exact balanced capacities per process
    static boolean canAssignExactBalanced(int n, int[] capacities) {
        int M = n * (n - 1) / 2;
        int S = 0;
        int T = M + n + 1;

        Dinic dinic = new Dinic(T + 1);

        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                pairs.add(new int[]{i, j});
            }
        }

        // S -> pairs
        for (int i = 0; i < M; i++) {
            dinic.addEdge(S, i + 1, 1);
        }

        // pairs -> processes
        for (int i = 0; i < M; i++) {
            int u = pairs.get(i)[0];
            int v = pairs.get(i)[1];
            dinic.addEdge(i + 1, M + 1 + u, 1);
            dinic.addEdge(i + 1, M + 1 + v, 1);
        }

        // processes -> T with exact capacities
        for (int i = 0; i < n; i++) {
            dinic.addEdge(M + 1 + i, T, capacities[i]);
        }

        int flow = dinic.maxFlow(S, T);
        return flow == M;
    }

 static void printAssignmentExactBalanced(int n, int[] capacities) {
    int M = n * (n - 1) / 2;
    int S = 0;
    int T = M + n + 1;

    Dinic dinic = new Dinic(T + 1);

    List<int[]> pairs = new ArrayList<>();
    for (int i = 0; i < n; i++) {
        for (int j = i + 1; j < n; j++) {
            pairs.add(new int[]{i, j});
        }
    }

    for (int i = 0; i < M; i++) {
        dinic.addEdge(S, i + 1, 1);
    }
    for (int i = 0; i < M; i++) {
        int u = pairs.get(i)[0];
        int v = pairs.get(i)[1];
        dinic.addEdge(i + 1, M + 1 + u, 1);
        dinic.addEdge(i + 1, M + 1 + v, 1);
    }
    for (int i = 0; i < n; i++) {
        dinic.addEdge(M + 1 + i, T, capacities[i]);
    }

    int flow = dinic.maxFlow(S, T);
    if (flow != M) {
        System.out.println("No assignment possible with given capacities");
        return;
    }

    List<List<int[]>> assignments = new ArrayList<>();
    for (int i = 0; i < n; i++) assignments.add(new ArrayList<>());

    for (int i = 0; i < M; i++) {
        for (Edge e : dinic.graph[i + 1]) {
            if (e.to >= M + 1 && e.to <= M + n && e.cap == 0) {
                int proc = e.to - (M + 1);
                assignments.get(proc).add(pairs.get(i));
                break;
            }
        }
    }

    System.out.println("\n--- Assignment Details ---");
    for (int i = 0; i < n; i++) {
        List<int[]> procPairs = assignments.get(i);
        procPairs.sort(Comparator.comparingInt(a -> a[0] * n + a[1])); // Optional: sort pairs for consistency

        System.out.printf("Process %3d (%2d pairs): ", i, procPairs.size());
        for (int j = 0; j < procPairs.size(); j++) {
            int[] p = procPairs.get(j);
            System.out.printf("(%d,%d)%s", p[0], p[1], (j < procPairs.size() - 1 ? ", " : ""));
        }
        System.out.println();
    }
}


    public static void main(String[] args) {
        int n = 100;
        int M = n * (n - 1) / 2;
        int q = M / n;
        int r = M % n;

        int[] capacities = new int[n];
        for (int i = 0; i < n; i++) {
            capacities[i] = q + (i < r ? 1 : 0);
        }

        System.out.println("Total pairs = " + M);
        System.out.println("Capacities per process (balanced): " + Arrays.toString(capacities));

        boolean possible = canAssignExactBalanced(n, capacities);
        System.out.println("Is exact balanced assignment possible? " + possible);

        if (possible) {
            System.out.println("Assignments:");
            printAssignmentExactBalanced(n, capacities);
        }
    }
}
