package gphhucarp.gp.terminal.feature;

import gphhucarp.core.Arc;
import gphhucarp.gp.CalcPriorityProblem;
import gphhucarp.gp.terminal.FeatureGPNode;

import java.util.List;

/**
 * The expected demand of the closest task to the candidate.
 * If there is no remaining task, return 0.
 *
 * Only considers the *first* task in the chain.
 */

public class Demand1 extends FeatureGPNode {
    public Demand1() {
        super();
        name = "DEM1";
    }

    @Override
    public double value(CalcPriorityProblem calcPriorityProblem) {
        List<Arc> candidate = calcPriorityProblem.getCandidate();

        List<Arc> taskAdjacentList = calcPriorityProblem.getState()
                .getTaskAdjacencyList(candidate.get(0));

        if (taskAdjacentList.isEmpty())
            return 0;

        Arc task1 = taskAdjacentList.get(0);

        return task1.getExpectedDemand();


        // original
//        Arc candidate = calcPriorityProblem.getCandidate();
//
//        List<Arc> taskAdjacentList = calcPriorityProblem.getState()
//                .getTaskAdjacencyList(candidate);
//
//        if (taskAdjacentList.isEmpty())
//            return 0;
//
//        Arc task1 = taskAdjacentList.get(0);
//
//        return task1.getExpectedDemand();
    }
}
