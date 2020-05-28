package gphhucarp.decisionprocess.routingpolicy;

import gphhucarp.core.Arc;
import gphhucarp.core.Graph;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.TieBreaker;
import gphhucarp.decisionprocess.poolfilter.ExpFeasiblePoolFilter;
import gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker;
import gphhucarp.representation.route.NodeSeqRoute;

import java.util.List;

/**
 * The path scanning 4 policy first selects the nearest neighbours.
 * Among multiple nearest neighbours,
 * it minimises the yield = demand/servCost
 */

public class PathScanning4Policy extends RoutingPolicy {
    // a sufficiently large coefficient to guarantee the priority of cost from here
    public static final double ALPHA = 10000;

    public PathScanning4Policy(PoolFilter poolFilter, TieBreaker tieBreaker) {
        super(poolFilter, tieBreaker);
        name = "\"PS4\"";
    }

    public PathScanning4Policy(TieBreaker tieBreaker) {
        this(new ExpFeasiblePoolFilter(), tieBreaker);
    }

    public PathScanning4Policy() {
        this(new SimpleTieBreaker());
    }

    @Override
    public double priority(List<Arc> candidate, NodeSeqRoute route, DecisionProcessState state) {
        Instance instance = state.getInstance();
        Graph graph = instance.getGraph();
        double costFromHere = graph.getEstDistance(route.currNode(), candidate.get(0).getFrom());
        double yield = state.getInstance().getActDemand(candidate.get(0)) / candidate.get(0).getServeCost();

        return ALPHA * costFromHere + yield;
    }
}
