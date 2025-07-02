package com.syntm.util;
// package com.syntm;
// import java.util.*;

// public class AgreementPartitionTest {

//     public static void main(String[] args) {
//         List<TestCase> tests = generateTestCases();
//         int passed = 0;

//         for (int i = 0; i < tests.size(); i++) {
//             TestCase test = tests.get(i);
//             try {
//                 List<Set<String>> result = ResponsibleAgreement.computeCliquePartition(test.input);
//                 if (validatePartition(result, test.input)) {
//                     System.out.println("✅ Test " + (i + 1) + " passed.");
//                     passed++;
//                 } else {
//                     System.out.println("❌ Test " + (i + 1) + " failed: invalid partition.");
//                 }
//             } catch (Exception e) {
//                 System.out.println("❌ Test " + (i + 1) + " failed with exception: " + e.getMessage());
//             }
//         }

//         System.out.println("\nPassed " + passed + " / " + tests.size() + " tests.");
//     }

//     static boolean validatePartition(List<Set<String>> partition, Map<String, Set<String>> S) {
//         Set<String> all = new HashSet<>();
//         for (Set<String> group : partition) {
//             for (String r1 : group) {
//                 for (String r2 : group) {
//                     if (!r1.equals(r2)) {
//                         if (!(S.get(r1).contains(r2) && S.get(r2).contains(r1))) {
//                             return false;
//                         }
//                     }
//                 }
//                 if (!all.add(r1)) return false; // duplicate
//             }
//         }
//         return all.equals(S.keySet());
//     }

//     static class TestCase {
//         Map<String, Set<String>> input;
//         TestCase(Map<String, Set<String>> input) {
//             this.input = input;
//         }
//     }

//     static List<TestCase> generateTestCases() {
//         List<TestCase> tests = new ArrayList<>();

//         // === TEST CASES ===
//         tests.add(new TestCase(mapOf(
//  "r1", set("r1", "r2"),
//             "r2", set("r2", "r1", "r3"),
//             "r3", set("r3", "r2")
//         ))); // Expect: {r1, r2}, {r3} or {r2, r3}, {r1}

//         tests.add(new TestCase(mapOf(
//  "a", set("a", "b", "c"),
//             "b", set("b", "a", "c"),
//             "c", set("c", "a", "b"),
//             "d", set("d", "e"),
//             "e", set("e", "d")
//         ))); // {a,b,c}, {d,e}

//         tests.add(new TestCase(mapOf(
//  "u", set("u", "v"),
//             "v", set("v", "w"),
//             "w", set("w", "x"),
//             "x", set("x", "w")
//         ))); //{u}, {v}, {x,w}

//         tests.add(new TestCase(mapOf(
//  "a", set("a", "b"),
//             "b", set("b", "c"),
//             "c", set("c", "a")
//         ))); // cycle, no clique

//         tests.add(new TestCase(mapOf(
//             "x", set("x"),
//             "y", set("y"),
//             "z", set("z")
//         ))); // all isolated

//         tests.add(new TestCase(mapOf(
//             "r1", set("r1", "r2"),
//             "r2", set("r2", "r3"),
//             "r3", set("r3", "r1")
//         ))); // triangle with no mutual agreement

//         tests.add(new TestCase(mapOf(
//             "r1", set("r1", "r2", "r3", "r4"),
//             "r2", set("r2", "r1", "r3", "r4"),
//             "r3", set("r3", "r1", "r2", "r4"),
//             "r4", set("r4", "r1", "r2", "r3")
//         ))); // full clique

//         tests.add(new TestCase(mapOf(
//             "a", set("a", "b"),
//             "b", set("b", "a"),
//             "c", set("c", "d"),
//             "d", set("d", "c"),
//             "e", set("e", "f", "g"),
//             "f", set("f", "e", "g"),
//             "g", set("g", "e", "f")
//         ))); // mix of pair and triplet cliques

//         // 12 more intricate mixed-partition tests
//         for (int i = 0; i < 12; i++) {
//             Map<String, Set<String>> custom = new LinkedHashMap<>();
//             int base = i * 3;
//             for (int j = 0; j < 3; j++) {
//                 String r = "r" + (base + j);
//                 Set<String> group = new HashSet<>();
//                 group.add(r);
//                 if (j > 0) group.add("r" + (base + j - 1));
//                 if (j < 2) group.add("r" + (base + j + 1));
//                 custom.put(r, group);
//             }
//             tests.add(new TestCase(custom));
//         }

//         return tests;
//     }

//     // === Utility helpers ===
//     static Set<String> set(String... elems) {
//         return new HashSet<>(Arrays.asList(elems));
//     }

//     static Map<String, Set<String>> mapOf(Object... entries) {
//         Map<String, Set<String>> map = new LinkedHashMap<>();
//         for (int i = 0; i < entries.length; i += 2) {
//             String key = (String) entries[i];
//             @SuppressWarnings("unchecked")
//             Set<String> value = (Set<String>) entries[i + 1];
//             map.put(key, value);
//         }
//         return map;
//     }
// }
