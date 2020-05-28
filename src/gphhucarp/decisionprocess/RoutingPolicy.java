package gphhucarp.decisionprocess;

import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.poolfilter.IdentityPoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker;
import gphhucarp.representation.route.NodeSeqRoute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A routing policy makes a decision
 */

public abstract class RoutingPolicy {

    protected String name;
    protected PoolFilter poolFilter;
    protected TieBreaker tieBreaker;

    public RoutingPolicy(PoolFilter poolFilter, TieBreaker tieBreaker) {
        this.poolFilter = poolFilter;
        this.tieBreaker = tieBreaker;
    }

    public RoutingPolicy(PoolFilter poolFilter) {
        this(poolFilter, new SimpleTieBreaker());
    }

    public RoutingPolicy(TieBreaker tieBreaker) {
        this(new IdentityPoolFilter(), tieBreaker);
    }

    public RoutingPolicy() {}

    public String getName() {
        return name;
    }

    public PoolFilter getPoolFilter() {
        return poolFilter;
    }

    public TieBreaker getTieBreaker() {
        return tieBreaker;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Given the current decison process state,
     * select the next task to serve by the give route from the pool of tasks.
     * @param rds the reactive decision situation.
     * @return the next task to be served by the route.
     */
    protected HashMap<List<Arc>, Double> priorities;
    protected List<List<Arc>> candidateChains;
    public List<Arc> next(ReactiveDecisionSituation rds, DecisionProcess dp) {
        List<Arc> pool = rds.getPool();
        NodeSeqRoute route = rds.getRoute();
        DecisionProcessState state = rds.getState();

        List<Arc> filteredPool = poolFilter.filter(pool, route, state);

        if (filteredPool.isEmpty())
            return null;

        priorities = new HashMap<>();
        candidateChains = new ArrayList<>();

        List<Arc> next = new ArrayList<>(); next.add(filteredPool.get(0));
        priorities.put(next, priority(next, route, state));

        for (int i = 1; i < filteredPool.size(); i++) {
            Arc tmp = filteredPool.get(i);
            List<Arc> tmpList = Stream.of(tmp).collect(Collectors.toList());
            priorities.put(tmpList, priority(tmpList, route, state));
//            tmp.setPriority(priority(tmp, route, state));

            if (Double.compare(priorities.get(tmpList), priorities.get(next)) < 0 ||
                    (Double.compare(priorities.get(tmpList), priorities.get(next)) == 0 &&
                            tieBreaker.breakTie(tmpList, next) < 0))
                next = tmpList;
        }

        return next;
    }

    /**
     * Given the current decision process state,
     * whether to continue the service of the planned task or not.
     * @param plannedChain the planned task to be served next.
     * @param route the current route.
     * @param state the decision process state.
     * @return true if continue to serve the planned task, and false otherwise.
     */
    public boolean continueService(List<Arc> plannedChain, NodeSeqRoute route, DecisionProcessState state) {
        if (priority(plannedChain, route, state) < 0)
            return false;

        return true;
    }

    /**
     * Calculate the priority of a candidate task for a route given a state.
     * @param chain the candidate chain.
     * @param route the route.
     * @param state the state.
     * @return the priority of the candidate task.
     */
    public abstract double priority(List<Arc> chain,
                                    NodeSeqRoute route,
                                    DecisionProcessState state);
}
