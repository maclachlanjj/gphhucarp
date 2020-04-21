package gphhucarp.gp.terminal.feature;

import gphhucarp.core.Arc;
import gphhucarp.core.Graph;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.gp.CalcPriorityProblem;
import gphhucarp.gp.terminal.FeatureGPNode;
import gphhucarp.representation.route.NodeSeqRoute;

/**
 * The cost from the closest feasible alternative route to the candidate task.
 * If the candidate is the depot loop, return 0 (no need to consider alternative route).
 * If there is no feasible alternative route, return infinity.
 *
 */

public class CostFromRoute1 extends FeatureGPNode {
    public CostFromRoute1() {
        super();
        name = "CFR1";
    }

    @Override
    public double value(CalcPriorityProblem calcPriorityProblem) {
        Arc candidate = calcPriorityProblem.getCandidate();

        if (candidate.equals(calcPriorityProblem.getState().getInstance().getDepotLoop()))
            return 0;

        DecisionProcessState state = calcPriorityProblem.getState();
        Instance instance = state.getInstance();
        Graph graph = instance.getGraph();
        int currNode = calcPriorityProblem.getRoute().currNode();
        int depot = instance.getDepot();

        if (state.getRouteAdjacencyList(candidate).isEmpty())
            return Double.POSITIVE_INFINITY;

        for (int i = 0; i < state.getRouteAdjacencyList(candidate).size(); i++) {
            NodeSeqRoute route1 = state.getRouteAdjacencyList(candidate).get(i);

            // whether the alternative is feasible or not
            if (route1.getDemand() + candidate.getExpectedDemand() <= route1.getCapacity()) {
                // yes, feasible
                int nextDecisionNode1 = route1.getNextTask().getTo();
                return graph.getEstDistance(nextDecisionNode1, candidate.getFrom());
            }
            else if (graph.getEstDistance(currNode, candidate.getFrom()) ==
                    graph.getEstDistance(currNode, depot) +
                            graph.getEstDistance(depot, candidate.getFrom())) {
                // pass depot, so can refill on the way
                int nextDecisionNode1 = route1.getNextTask().getTo();
                return graph.getEstDistance(nextDecisionNode1, candidate.getFrom());
            }
        }

        // no alternative route is feasible
        return Double.POSITIVE_INFINITY;
    }
}
