package gphhucarp.gp.terminal.feature;

import gphhucarp.core.Arc;
import gphhucarp.gp.CalcPriorityProblem;
import gphhucarp.gp.terminal.FeatureGPNode;
import gphhucarp.representation.route.NodeSeqRoute;

import java.util.List;

/**
 * The remaining capacity of the closest alternative route to the candidate task.
 * If there is no alternative route, return 0.
 *
 * JJM: Currently only considers the *first* task in the chain.
 */

public class RemainingCapacity1 extends FeatureGPNode {
    public RemainingCapacity1() {
        super();
        name = "RQ1";
    }

    @Override
    public double value(CalcPriorityProblem calcPriorityProblem) {
        List<Arc> candidate = calcPriorityProblem.getCandidate();

        if (calcPriorityProblem.getState()
                .getRouteAdjacencyList(candidate.get(0)).isEmpty())
            return 0;

        NodeSeqRoute route1 = calcPriorityProblem.getState()
                .getRouteAdjacencyList(candidate.get(0)).get(0);

        return route1.getCapacity() - route1.getDemand();

        // original:
//        Arc candidate = calcPriorityProblem.getCandidate();
//
//        if (calcPriorityProblem.getState()
//                .getRouteAdjacencyList(candidate).isEmpty())
//            return 0;
//
//        NodeSeqRoute route1 = calcPriorityProblem.getState()
//                .getRouteAdjacencyList(candidate).get(0);
//
//        return route1.getCapacity() - route1.getDemand();
    }
}
