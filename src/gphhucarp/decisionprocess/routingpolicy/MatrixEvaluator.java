package gphhucarp.decisionprocess.routingpolicy;

import ec.EvolutionState;
import ec.gp.GPTree;
import ec.util.Parameter;
import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.gp.CalcPriorityProblem;
import gphhucarp.representation.route.NodeSeqRoute;
import gputils.DoubleData;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MatrixEvaluator extends DualTree_GPRoutingPolicy {

    public MatrixEvaluator() { super(); }

    public MatrixEvaluator(PoolFilter pf, GPTree[] gpTrees){
        super(pf, gpTrees);
    }

    /**
     * Version four of this algorithm: 489
     *
     * @param U
     * @param dp
     */
    private HashMap<NodeSeqRoute, ArrayList<Tuple>> routeQueues;
    public void updateMatrix(List<Arc> U, DecisionProcess dp){
        DecisionProcessState state = dp.getState();
        List<NodeSeqRoute> R = state.getSolution().getRoutes();
        GPTree[] trees = this.getGPTrees();

        // to avoid checking if every route is present before clearing.
        if(routeQueues == null)
            routeQueues = new HashMap<>();
        else
            routeQueues.clear();

        DoubleData data = new DoubleData();
        CalcPriorityProblem calcPrioProb;

        for(NodeSeqRoute r: R){
            ArrayList<Tuple> routeQueue = new ArrayList<>();
            for(Arc a: U){
                List<Arc> l = new ArrayList<>(); l.add(a);
                calcPrioProb = new CalcPriorityProblem(l, r, state);
                trees[1].child.eval(null, 0, data, null, null, calcPrioProb);
                routeQueue.add(new Tuple(a, data.value));
            }
            routeQueues.put(r, routeQueue);
        }

        constructCandidateSubset_greedy(dp);
    }

    private HashMap<NodeSeqRoute, HashSet<Arc>> candidateSubsets;
    private void constructCandidateSubset_greedy(DecisionProcess dp){
        DecisionProcessState state = dp.getState();
        List<NodeSeqRoute> R = state.getSolution().getRoutes();
        double Q = state.getInstance().getCapacity();

        if(candidateSubsets == null)
            candidateSubsets = new HashMap<>();

        for(NodeSeqRoute r: R) {
            candidateSubsets.put(r, new HashSet<>());
            routeQueues.get(r).sort(Tuple::compareTo);
        }

        HashSet<Arc> taken = new HashSet<>();

        for(NodeSeqRoute r: R){
            double rq = Q - r.getDemand(); // route's current remaining capacity
            ArrayList<Tuple> pq = routeQueues.get(r);
            HashSet<Arc> subset = candidateSubsets.get(r);

            while(pq.size() > 0) {
                Tuple t = pq.remove(0);

                if (!taken.contains(t.a))
                    if (t.a.getExpectedDemand() <= rq) {
                        subset.add(t.a);
                        taken.add(t.a);
                    }
            }
        }
    }

    /**
     * Viewing the candidate subset construction as an assignment problem,
     * where we construct a Basic Feasible Solution via Vogel's approximation
     * algorithm.
     */
    private void constructCandidateSubset_bfs(DecisionProcess dp){
        DecisionProcessState state = dp.getState();
        List<NodeSeqRoute> R = state.getSolution().getRoutes();
        List<Arc> U = state.getUnassignedTasks();
        double Q = state.getInstance().getCapacity();

        HashMap<NodeSeqRoute, Double> remCaps; // stores the route's remaining 'supply'
        HashMap<Arc, Double> remDems; // stores the task's remaining 'demand'

        remCaps = new HashMap<>();
        double sumRemQ = 0;
        for(NodeSeqRoute r: R) {
            double remQ = Q - r.getDemand(); // the route's current remaining demand
            remCaps.put(r, remQ);
            sumRemQ += remQ;
        }

        remDems = new HashMap<>();
        double sumRemDem = 0;
        for(Arc a: state.getUnassignedTasks()){
            remDems.put(a, a.getExpectedDemand());
            sumRemDem += a.getExpectedDemand();
        }

        NodeSeqRoute fauxRoute;
        Arc fauxTask;
        int sumCase;
        if(sumRemDem > sumRemQ){
            // add a faux vehicle
            sumCase = 0;
            fauxRoute = new NodeSeqRoute(sumRemDem - sumRemQ);
            remCaps.put(fauxRoute, sumRemDem - sumRemQ);
        } else if(sumRemDem < sumRemQ){
            // add a faux task
            sumCase = 1;
            fauxTask = new Arc(0,0,sumRemQ - sumRemDem,0,0,null,0,0);
            remDems.put(fauxTask, sumRemQ - sumRemDem);
        } else {
            // don't add anything
            sumCase = 2;
        }

        HashMap<NodeSeqRoute, Double> RDi = new HashMap<>();    // the row differences
        HashMap<Arc, Double> CDi = new HashMap<>();             // the column differences

        calcRowColDiffs(RDi, CDi, R, U);

        // not finished. Need to do the following:
        // - set to M (infinity) the priorities of task/veh pairs where the vehicle can't feasibly serve the task
        // - all details beyond the first step, including the above while on the fly.

    }

    public void calcRowColDiffs(HashMap<NodeSeqRoute, Double> RDi, HashMap<Arc, Double> CDi, List<NodeSeqRoute> R, List<Arc> U){
        for(NodeSeqRoute r: R){
            double minnest = Double.POSITIVE_INFINITY;
            double minner = Double.POSITIVE_INFINITY;
            for(Tuple t: routeQueues.get(r)){
                if(Double.compare(t.p, minnest) < 0) {
                    minner = minnest;
                    minnest = t.p;
                }
            }

            RDi.put(r, minner - minnest);
        }

        for(int i = 0; i < U.size(); i++){
            double minnest = Double.POSITIVE_INFINITY;
            double minner = Double.POSITIVE_INFINITY;

            Arc a = null;
            for(NodeSeqRoute r: R){
                a = routeQueues.get(r).get(i).a;
                double val = routeQueues.get(r).get(i).p;
                if(Double.compare(val, minnest) < 0) {
                    minner = minnest;
                    minnest = val;
                }
            }

            CDi.put(a, minner - minnest);
        }
    }

    @Override
    public List<Arc> next(ReactiveDecisionSituation rds, DecisionProcess dp){
        double startTime = System.currentTimeMillis();
        NodeSeqRoute r = rds.getRoute();
        DecisionProcessState state = rds.getState();

        updateMatrix(state.getUnassignedTasks(), dp);

        // why candidateSubsets.get(r)?
//        ArrayList<Arc> pool = new ArrayList<>(candidateSubsets.get(r));

        // not: state.getUnassignedTasks() + candidateSubsets.get(r)
        ArrayList<Arc> pool = new ArrayList<>(candidateSubsets.get(r));
        pool.addAll(state.getUnassignedTasks());

        List<Arc> filteredPool = poolFilter.filter(pool, r, state);

        if (filteredPool.isEmpty())
            return null;

        priorities = new HashMap<>();

        List<Arc> next = Stream.of(filteredPool.get(0)).collect(Collectors.toList());
        priorities.put(next, priority(next, r, state));

        for (int i = 1; i < filteredPool.size(); i++) {
            List<Arc> tmp = Stream.of(filteredPool.get(i)).collect(Collectors.toList());
            priorities.put(tmp, priority(tmp, r, state));

            if (Double.compare(priorities.get(tmp), priorities.get(next)) < 0 ||
                    (Double.compare(priorities.get(tmp), priorities.get(next)) == 0 &&
                            tieBreaker.breakTie(tmp, next) < 0))
                next = tmp;
        }

        double totalTime = System.currentTimeMillis() - startTime;
        sumDecisionTime += totalTime; numDecisions++;

        return next;
    }

    @Override
    public void setup(EvolutionState state, Parameter base) {

    }

    @Override
    public GPRoutingPolicy_frame newTree(PoolFilter poolFilter, GPTree[] gpTrees) {
        return new MatrixEvaluator(poolFilter, gpTrees);
    }

    @Override
    public boolean recordingData() {
        return false;
    }

    public static class Tuple implements Comparable{
        private Arc a;
        private Double p;

        public Tuple(Arc arc, double priority) {
            a = arc;
            p = priority;
        }

        public Arc getA() {
            return a;
        }

        public Double getP() {
            return p;
        }

        @Override
        public int compareTo(Object o) {
            Tuple other = (Tuple) o;
            return Double.compare(this.p, other.p);
        }
    }
}
