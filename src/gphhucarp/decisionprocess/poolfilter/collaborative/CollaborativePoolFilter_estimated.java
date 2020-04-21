package gphhucarp.decisionprocess.poolfilter.collaborative;

import gphhucarp.core.Arc;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.DecisionProcessState;
import gputils.Gaussian;

/**
    Collaborative filter: estimate the remaining demand given the amount previously served
    and the known distribution.
 */

public class CollaborativePoolFilter_estimated extends CollaborativeFilter {
    @Override
    public double getKnownDemand(Arc task, Instance i, DecisionProcessState s){
        double mu = task.getExpectedDemand();
        double sigma = i.getActDemand(task);
        double a = i.getActDemand(task) * (1 - s.getTaskRemainingDemandFrac(task)); // effective min is what has already been served.
        double alpha = (a - mu) / sigma;

        double newMu = mu + sigma *
                (Gaussian.pdf(alpha) /
                        (1 - Gaussian.cdf(alpha)));

        return newMu - a;
    }
}
