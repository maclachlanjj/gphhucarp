package gphhucarp.gp.terminal.feature;

import gphhucarp.core.Arc;
import gphhucarp.gp.CalcPriorityProblem;
import gphhucarp.gp.terminal.FeatureGPNode;

/**
 * Feature: the deadheading cost of the candidate task.
 *
 * Created by gphhucarp on 30/08/17.
 */
public class DeadheadingCost extends FeatureGPNode {
    public DeadheadingCost() {
        super();
        name = "DC";
    }

    @Override
    public double value(CalcPriorityProblem calcPriorityProblem) {
        double res = 0;
        for(Arc a: calcPriorityProblem.getCandidate())
            res += a.getExpectedDeadheadingCost();

        return res;

        // original
//        return calcPriorityProblem.getCandidate().getExpectedDeadheadingCost();
    }
}
