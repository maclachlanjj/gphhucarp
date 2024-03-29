package gphhucarp.gp.terminal.feature;

import gphhucarp.core.Arc;
import gphhucarp.core.Graph;
import gphhucarp.gp.CalcPriorityProblem;
import gphhucarp.gp.terminal.FeatureGPNode;

import java.util.List;

/**
 * The fullness after serving the task.
 */

public class FullnessAfterService extends FeatureGPNode {

    public FullnessAfterService() {
        super();
        name = "FAS";
    }

    @Override
    public double value(CalcPriorityProblem calcPriorityProblem) {
        double routeDemand = calcPriorityProblem.getRoute().getDemand();
        Graph graph = calcPriorityProblem.getState().getInstance().getGraph();
        int currNode = calcPriorityProblem.getRoute().currNode();
        List<Arc> candidate = calcPriorityProblem.getCandidate();
        int depot = calcPriorityProblem.getState().getInstance().getDepot();

        Arc firstInChain = candidate.get(0);

        // if depot is passed along the way, then refill on the way
        // after the service, the route demand will only consists of the task demand
        if (graph.getEstDistance(currNode, firstInChain.getFrom()) ==
                graph.getEstDistance(currNode, depot) +
                        graph.getEstDistance(depot, firstInChain.getFrom())) {
            return firstInChain.getExpectedDemand() / calcPriorityProblem.getRoute().getCapacity();
        }

        // otherwise, increment
        return (routeDemand + firstInChain.getExpectedDemand()) / calcPriorityProblem.getRoute().getCapacity();

        // original
//        double routeDemand = calcPriorityProblem.getRoute().getDemand();
//        Graph graph = calcPriorityProblem.getState().getInstance().getGraph();
//        int currNode = calcPriorityProblem.getRoute().currNode();
//        Arc candidate = calcPriorityProblem.getCandidate();
//        int depot = calcPriorityProblem.getState().getInstance().getDepot();
//
//        // if depot is passed along the way, then refill on the way
//        // after the service, the route demand will only consists of the task demand
//        if (graph.getEstDistance(currNode, candidate.getFrom()) ==
//                graph.getEstDistance(currNode, depot) +
//                        graph.getEstDistance(depot, candidate.getFrom())) {
//            return candidate.getExpectedDemand() / calcPriorityProblem.getRoute().getCapacity();
//        }
//
//        // otherwise, increment
//        return (routeDemand + candidate.getExpectedDemand()) / calcPriorityProblem.getRoute().getCapacity();
    }
}
