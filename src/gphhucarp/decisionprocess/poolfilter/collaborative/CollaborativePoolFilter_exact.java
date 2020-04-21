package gphhucarp.decisionprocess.poolfilter.collaborative;

import gphhucarp.core.Arc;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.DecisionProcessState;

/**
    Collaborative filter: if a task has been previously served, we know its exact remaining demand.
 */

public class CollaborativePoolFilter_exact extends CollaborativeFilter {
    @Override
    public double getKnownDemand(Arc task, Instance i, DecisionProcessState s){
        return s.getTaskRemainingDemandFrac(task) * i.getActDemand(task);
    }
}
