package gphhucarp.gp.terminal.feature;

import gphhucarp.core.Arc;
import gphhucarp.gp.CalcPriorityProblem;
import gphhucarp.gp.terminal.FeatureGPNode;

/**
 * Feature: the expected demand of the candidate task.
 *
 * Created by gphhucarp on 30/08/17.
 */
public class Demand extends FeatureGPNode {
    public Demand() {
        super();
        name = "DEM";
    }

    @Override
    public double value(CalcPriorityProblem calcPriorityProblem) {
        double res = 0;
        for(Arc a: calcPriorityProblem.getCandidate())
            res += a.getExpectedDemand();
        return res;

        // original
//        return calcPriorityProblem.getCandidate().getExpectedDemand();
    }
}
