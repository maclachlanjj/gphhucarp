package gphhucarp.decisionprocess.routingpolicy;


import ec.EvolutionState;
import ec.gp.GPTree;
import ec.util.Parameter;
import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.representation.route.NodeSeqRoute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChainPolicy extends GPRoutingPolicy_frame {
    public static final String P_CHAINLENGTH = "chain-length";
    private static int chainlength;

    public ChainPolicy(){ super(); }

    public ChainPolicy(PoolFilter poolFilter, GPTree[] gpTrees) {
        super(poolFilter, gpTrees);
    }

    @Override
    public void setup(EvolutionState state, Parameter base) {
        Parameter p = base.push(P_CHAINLENGTH);
        chainlength = state.parameters.getIntWithDefault(p, null, 1);
        // chain length defaults to 1 - i.e. normal GPHH
    }

    @Override
    public GPRoutingPolicy_frame newTree(PoolFilter poolFilter, GPTree[] gpTrees) {
        return new ChainPolicy(poolFilter, gpTrees);
    }

    @Override
    public boolean recordingData() {
        return false;
    }

    @Override
    public List<Arc> next(ReactiveDecisionSituation rds, DecisionProcess dp) {
        NodeSeqRoute r = rds.getRoute();
        DecisionProcessState state = rds.getState();

        List<Arc> pool = poolFilter.filter(state.getUnassignedTasks(), r, state);

        if(pool.isEmpty())
            return null;

        double remQ = r.getCapacity() - r.getDemand();

        List<Arc> seq; List<Arc> set;

        candidateChains = new ArrayList<>();

        for(Arc a: pool){
            seq = Stream.of(a).collect(Collectors.toList()); // makes a new List<Arc> with a present
            candidateChains.add(seq);
            double currDemand = a.getExpectedDemand();

            set = new ArrayList<>(pool); set.remove(a); set.remove(a.getInverse());

            generatePermutations(candidateChains, seq, set, currDemand, remQ);
        }

//        System.out.println("|S|: " + pool.size());
//        System.out.println("|C|: " + candidateChains.size());

        priorities = new HashMap<>();

        List<Arc> next = candidateChains.get(0);
        priorities.put(next, priority(next, r, state));

        for (int i = 1; i < candidateChains.size(); i++) {
            List<Arc> tmp = candidateChains.get(i);
            priorities.put(tmp, priority(tmp, r, state));

            if (Double.compare(priorities.get(tmp), priorities.get(next)) < 0 ||
                    (Double.compare(priorities.get(tmp), priorities.get(next)) == 0 &&
                            tieBreaker.breakTie(tmp, next) < 0))
                next = tmp;
        }

//        String s = "Chain: \t";
//        for (Arc a : next)
//            s += a.getFrom() + " - " + a.getTo() + " \t | \t";
//
//        System.out.println(s + " route: " + r.hashCode());

        return next;
    }

    /**
     * From the filtered pool of singleton candidate tasks, generate a set of permutations
     * up to size *chainlength* that this vehicle has the capacity to serve.
     */
    private void generatePermutations(List<List<Arc>> candidateChains, List<Arc> seq, List<Arc> set, double currDemand, double remQ){
        List<Arc> tempSet;
        List<Arc> tempSeq;

        for(Arc a: set){
            double propDem = currDemand + a.getExpectedDemand();
            if(propDem <= remQ) {
                tempSeq = new ArrayList<>(seq); tempSeq.add(a);
                candidateChains.add(tempSeq);

                tempSet = new ArrayList<>(set); tempSet.remove(a); tempSet.remove(a.getInverse());

                if(tempSeq.size() < chainlength)
                    generatePermutations(candidateChains, tempSeq, tempSet, propDem, remQ);
            }
        }
    }

    // need a custom priority method and to re-do all the terminals
}


