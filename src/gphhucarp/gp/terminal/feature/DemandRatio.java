package gphhucarp.gp.terminal.feature;

import gphhucarp.core.Arc;
import gphhucarp.gp.CalcPriorityProblem;
import gphhucarp.gp.terminal.FeatureGPNode;

/**
 * The demand ratio of a task: demand / capacity of the route
 *
 * JJM (without implementing) on 2020/05/25: is this intended to be the *live* route capacity, or general? Currently general.
 */

public class DemandRatio extends FeatureGPNode {
    public DemandRatio() {
        super();
        name = "DR";
    }

    @Override
    public double value(CalcPriorityProblem calcPriorityProblem) {
        double dem = 0;
        for(Arc a: calcPriorityProblem.getCandidate())
            dem += a.getExpectedDemand();
        return dem / calcPriorityProblem.getRoute().getCapacity();

        // original
//        return calcPriorityProblem.getCandidate().getExpectedDemand() /
//                calcPriorityProblem.getRoute().getCapacity();
    }
}
