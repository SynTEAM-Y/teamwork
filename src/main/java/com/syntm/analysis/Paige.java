package com.syntm.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.syntm.lts.CompressedTS;
import com.syntm.lts.State;
import com.syntm.lts.TS;
import com.syntm.lts.Trans;

public class Paige implements java.io.Serializable {
    private Set<String> channels;
    private TS transSystem;
    private Map<State, Set<Set<State>>> epsilonMap;

    /**
     * Contructor for a class using the Paige-Tarjan algorithm to reduce the
     * transition system.
     * 
     * @param parameterStates All states in the parameter.
     * @param indexedFamily   A concurrent hashmap with an initial partition mapped
     *                        to each state.
     * @param transSystem     The transition system to be reduced.
     * @param channels        A set of the available channels.
     * @return An object callable with <code>run()</code> to reduce the transition
     *         system using the Paige-Tarjan algorithm.
     */
    public Paige(Map<State, Set<Set<State>>> epsilonMap,
            TS transSystem, Set<String> channels) {
        this.epsilonMap = new HashMap<>(epsilonMap);
        this.transSystem = transSystem;
        this.channels = channels; // Space complexity O(c)

        // Add all incoming transitions to each state.
        // Should be moved to wherever states are defined but
        // it's impossible to find where that is.
        for (Trans tr : transSystem.getTransitions()) { // O(m)
            tr.destination.getPreTrans().add(tr);
        }

    }

    /**
     * 
     * @return
     */
    public TS run() {
        boolean fixedPoint = false;
        while (!fixedPoint) { // O(c^2mn^4)
            HashMap<State, Set<Set<State>>> rhoResults = new HashMap<>();

            for (State epsilon : epsilonMap.keySet()) {
                Set<Set<State>> rhoTemp = new HashSet<>(epsilonMap.get(epsilon));
                Set<Set<State>> x = new HashSet<>();
                Set<State> rhoBlockTemp = new HashSet<>();

                for(Set<State> rhoBlock : rhoTemp ){
                    rhoBlockTemp.addAll(rhoBlock);
                }
                x.add(rhoBlockTemp);
                Set<Set<State>> piInit = new HashSet<>(rhoTemp);
                Set<Set<State>> pi = preProcessRho(piInit);
                Set<Set<State>> piTemp =  new HashSet<>(pi);

                int n = 0;

                while (!pi.equals(x)) {

                    
                    // Step 1
                    Set<Set<State>> xTemp = new HashSet<>(x);
                    xTemp.removeAll(pi);
                    Set<State> s = xTemp.iterator().next();
                    Set<State> b = new HashSet<>();
                    
                    // Step 2
                    for (Set<State> bTemp : pi) {
                        if (!bTemp.isEmpty() && s.containsAll(bTemp) && (bTemp.size() < s.size() / 2)) {
                            b = bTemp;
                            break;
                        }
                    }

                    if (!b.isEmpty()) {
                        // Step 3
                        x.remove(s);
                        x.add(b);
                        Set<State> sTemp = new HashSet<>(s);
                        sTemp.removeAll(b);
                        x.add(sTemp);
                        System.out.println(n);
                        n++;

                        for (String channel : this.channels) {
                            for (Trans trEpsilon : epsilon.getTrans()) {
                                Set<Set<State>> ePartitions = new HashSet<>(this.epsilonMap.get(trEpsilon.getDestination()));
                                for (Set<State> ePrime : ePartitions) {

                            // This is step 4
                                    Set<State> splitter = applyEBisim(b, trEpsilon, ePrime, channel, epsilon); // O(mn)
                                    if (!splitter.isEmpty() && !splitter.equals(b)) {
                                        // Set<Set<State>> splitP = threeSplit(pi, splitter, s, channel); 
                                        Set<Set<State>> splitP = split(b, splitter);
                                        piTemp.remove(b);
                                        piTemp.addAll(splitP);
                                        rhoTemp.remove(b);
                                        rhoTemp.addAll(splitP);
                                    }
                                }
                            }
                        }
                    }
                    pi = piTemp;
                }
                rhoResults.put(epsilon, rhoTemp);
            }

            // update epsilonmap with the new partitions
            fixedPoint = true;
            for (State e : rhoResults.keySet()) {
                if (!epsilonMap.get(e).equals(rhoResults.get(e))) {
                    // not a fixed point, repeat
                    fixedPoint = false;
                    epsilonMap.put(e, rhoResults.get(e));
                }
            }
        }

        Set<Set<State>> rhoFinal = new HashSet<>();
        for (State s : epsilonMap.keySet()) {
            // Gets the partition from the initial state.
            if (this.transSystem.initStateEqLabel(s)) {
                if (epsilonMap.get(s).size() == 1)
                    return this.transSystem;
                rhoFinal.addAll(epsilonMap.get(s));
                break;
            }
        }

        CompressedTS c = new CompressedTS("s-" + this.transSystem.getName());
        return c.compressedTS(this.transSystem, rhoFinal);
    }

    /**
     * Def 5.4 in the paper
     * <p>
     * Complexity O(mn)
     * 
     * @param p         The starting block
     * @param trEpsilon A transition (from p to ePrime)
     * @param ePrime    The block where the transition ends up.
     * @param channel   A channel name
     * @param epsilon   A state (our addition)
     * @return A possible splitter for the block.
     */
    private Set<State> applyEBisim(Set<State> p, Trans trEpsilon, // O(c+mn)=O(mn) or O(cn+mn+n^2)=O(mn)
            Set<State> ePrime, String channel,
            State epsilon) {
        Set<State> out = new HashSet<>();

        if (epsilon.enable(epsilon, channel) // activating a channel, adds O(c)
                && trEpsilon.getAction().equals(channel)) {
            for (State s : p) { // *O(mn) or O(cn + n*max(c, m+n)) = O(max(cn,
                                // cn+mn+n^2))=O(cn+mn+n^2)=O(mn+n^2)=O(mn) but combines with the partition for
                                // loop outside the function
                for (State sPrime : p) { // O(c + max(c, m+n)), c+m+n <= 3m -> O(m)

                    if (s.canExactSilent(s.getOwner(), s, channel) // O(c)
                            && !sPrime.canDirectReaction(sPrime.getOwner(), sPrime, channel)) {
                        if (sPrime.canExactSilent(sPrime.getOwner(), sPrime, channel)) { // O(c)
                            if (ePrime.contains(s.takeExactSilent(s.getOwner(), s,
                                    channel).getDestination())
                                    && ePrime.contains(sPrime.takeExactSilent(sPrime.getOwner(), sPrime,
                                            channel).getDestination())) {
                                out.add(s);
                                out.add(sPrime);
                            }
                        } else { // O(c)
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

                    if (!s.getListen().getChannels().contains(channel) && // O(1)
                            !sPrime.getListen().getChannels().contains(channel)) {
                        if (ePrime.contains(s) && ePrime.contains(sPrime)) {
                            out.add(s);
                            out.add(sPrime);
                        }
                    }

                    if (s.canDirectReaction(s.getOwner(), s, channel) // O(m+n) -- it is O(max(c, m+n)) = O(m+n) because
                                                                      // c<=m
                            && !sPrime.canExactSilent(sPrime.getOwner(), sPrime, channel)) {
                        if (sPrime.canDirectReaction(sPrime.getOwner(), sPrime, channel)) { // O(c)
                            if (ePrime.contains(s.takeDirectReaction(s.getOwner(), s, channel).getDestination())
                                    && ePrime
                                            .contains(sPrime.takeDirectReaction(sPrime.getOwner(), sPrime, channel)
                                                    .getDestination())) {
                                out.add(s);
                                out.add(sPrime);
                            }
                        } else { // O(m+n)
                            Set<State> reach = sPrime.weakBFS(sPrime.getOwner(), sPrime, channel); // O(m+n)
                            if (reach.size() != 1) { // O(m)
                                for (State sReach : reach) { // O(m) It should be O(cn) but it's bounded by the number
                                                             // of transitions. It never checks a transition twice so it
                                                             // becomes O(m).
                                    if (sReach.canDirectReaction(sReach.getOwner(), sReach, channel)) { // O(c)
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

        if (out.isEmpty() && !epsilon.canTakeInitiative(epsilon.getOwner(), epsilon, channel) // O(cn)
                && epsilon.getListen().getChannels().contains(channel)) {
            for (State s : p) { // O(cn)
                if (s.canTakeInitiative(s.getOwner(), s, channel)) {
                    if (ePrime.contains(s.takeInitiative(s.getOwner(), s, channel).getDestination())) {
                        out.add(s);
                    }
                }
            }
        }

        if (out.isEmpty() && !epsilon.getListen().getChannels().contains(channel)) { // O(cn)
            for (Set<State> partition : epsilonMap.get(epsilon)) { // Still O(cn)
                for (State s : p) { // O(cn)
                    if (s.canTakeInitiative(s.getOwner(), s, channel)) {
                        if (partition.contains(s.takeInitiative(s.getOwner(), s, channel).getDestination())) {
                            out.add(s);
                        }
                    }
                }
                if (!out.isEmpty() && !out.equals(p))
                    break;
                if (out.equals(p))
                    out.clear();
            }
        }

        return out;
    }

    /**
     * A splitter function
     * <p>
     * Time complexity O(n)
     * 
     * @param p        The block to split
     * @param splitter The block to split with
     * @return The resulting blocks
     */
    private Set<Set<State>> split(Set<State> p, Set<State> splitter) {
        Set<Set<State>> splitP = new HashSet<>();
        Set<State> notsplitter = new HashSet<>(p);

        notsplitter.removeAll(splitter);
        splitP.add(splitter);
        splitP.add(notsplitter);

        return splitP;
    }

    /**
     * pre(a, S) = {s | s a→ s′ for some s′ ∈ S}
     * <p>
     * Complexity O(nm) (with only one block's worth of n)
     * 
     * @param channel The channel to find transitions for
     * @param block   The block with incoming transitions
     * @return A set of all states with transitions to the block
     */
    private Set<State> pre(Set<State> block) {
        Set<State> finalSet = new HashSet<>();
        for (State s : block) {
            for (Trans trans : s.getPreTrans()) {
                    finalSet.add(trans.getSource());            
                }
        }
        return finalSet;
    }

    private Set<State> pre2(Set<Set<State>> partition) {
        Set<State> finalSet = new HashSet<>();
        for (Set<State> sBlock : partition) {
            finalSet.addAll(pre(sBlock));
        }

        return finalSet;
    }

    /**
     * A set B ⊆ Pr is stable with respect to a set S ⊆ Pr if
     * either B ⊆ pre(S) or B ∩ pre(S) = ∅.
     * 
     * @param channel The channel to check stability on
     * @param sBlock  Set S
     * @param bBlock  Set B
     * @return <code>true</code> if the set B is stable
     */
    private boolean isStable(Set<State> sBlock, Set<State> bBlock) {
        Set<State> s = pre(sBlock);
        Set<State> intersection = new HashSet<>(s);
        intersection.retainAll(bBlock);
        return (intersection.isEmpty() || s.containsAll(bBlock));
    }

    /**
     * A partition π is stable with respect to a set S if so is each block B ∈ π.
     * 
     * @param channel
     * @param partition
     * @param sBlock
     * @return
     */
    private boolean partitionIsStable(String channel, Set<Set<State>> partition, Set<State> sBlock) {
        for (Set<State> b : partition) {
            if (!isStable(sBlock, b))
                return false;
        }
        return true;
    }

    /**
     * A partition π is stable with respect to a partition π′ if π is stable with
     * respect to each block in π′.
     * 
     * @param channel
     * @param partition1
     * @param partition2
     * @return
     */
    private boolean partitionIsStableToPartition(String channel, Set<Set<State>> partition1,
            Set<Set<State>> partition2) {
        for (Set<State> b : partition2) {
            if (!partitionIsStable(channel, partition1, b))
                return false;
        }
        return true;
    }

    /**
     * A partition π is stable if it is stable with respect to itself.
     * 
     * @param channel
     * @param partition
     * @return
     */
    private boolean isStableToItself(String channel, Set<Set<State>> partition) {
        return partitionIsStableToPartition(channel, partition, partition);
    }

    private Set<Set<State>> preProcessRho(Set<Set<State>> x) {
        Set<Set<State>> xTemp = new HashSet<>(x);

        Set<Set<State>> rhoTemp = new HashSet<>();

        for (Set<State> b : xTemp) {
            Set<State> bTemp = new HashSet<>(b);
            Set<State> bTemp2 = new HashSet<>(b);


            Set<State> preTemp1 = new HashSet<>(pre2(xTemp));



            bTemp.retainAll(preTemp1);
            if(!bTemp.isEmpty()){
                rhoTemp.add(bTemp);
            }
            Set<State> preTemp2 = new HashSet<>(pre(b));

            bTemp2.removeAll(preTemp2);

            if(!bTemp2.isEmpty()){
                rhoTemp.add(bTemp2);
            }
        }
        // System.out.println("RhoTemp: "+ rhoTemp);
        return rhoTemp;
    }

    // Kan vara så att pre i threeSplit måste innehålla channel
    private Set<Set<State>> threeSplit(Set<Set<State>> pi, Set<State> b, Set<State> s, String channel) {
        Set<Set<State>> finalPartition = new HashSet<>();
        for (Set<State> d : pi) {
            if (!isStable(d, b)) {
                Set<State> d1 = new HashSet<>(d);
                Set<State> d2 = new HashSet<>(d);
                d1.retainAll(pre(b));
                d2.removeAll(pre(b));

                s.removeAll(b);
                finalPartition.add(d1);
                if (!isStable(d1, s)) {
                    Set<State> d11 = new HashSet<>(d1);
                    Set<State> d12 = new HashSet<>(d1);
                    d11.retainAll(s);
                    d12.removeAll(pre(s));
                    finalPartition.add(d11);
                    finalPartition.add(d12);
                } else {
                    finalPartition.add(d2);
                }

            }
        }
        if (finalPartition.isEmpty()) {
            return pi;
        }
        return finalPartition;

    }


}
