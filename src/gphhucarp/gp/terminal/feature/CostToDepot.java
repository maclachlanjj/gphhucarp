package gphhucarp.gp.terminal.feature;

import gphhucarp.core.Arc;
import gphhucarp.core.Instance;
import gphhucarp.representation.route.NodeSeqRoute;
import gphhucarp.gp.CalcPriorityProblem;
import gphhucarp.gp.terminal.FeatureGPNode;

import java.util.List;

/**
 * Feature: the cost from the tail node of the task to the depot.
 *
 * Created by gphhucarp on 31/08/17.
 *
 * Edited by JJM 2020/05/25: consider the tail node of the last task in the chain.
 */
public class CostToDepot extends FeatureGPNode {

    public CostToDepot() {
        super();
        name = "CTD";
    }

    @Override
    public double value(CalcPriorityProblem calcPriorityProblem) {
        Instance instance = calcPriorityProblem.getState().getInstance();
        NodeSeqRoute route = calcPriorityProblem.getRoute();
        List<Arc> candidate = calcPriorityProblem.getCandidate();
        return instance.getGraph().getEstDistance(candidate.get(candidate.size()-1).getTo(), instance.getDepot());

        // original
//        Instance instance = calcPriorityProblem.getState().getInstance();
//        NodeSeqRoute route = calcPriorityProblem.getRoute();
//        Arc candidate = calcPriorityProblem.getCandidate();
//        return instance.getGraph().getEstDistance(candidate.getTo(), instance.getDepot());
    }
}
