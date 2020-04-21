package gphhucarp.decisionprocess.poolfilter.collaborative;

import gphhucarp.core.Arc;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.representation.route.NodeSeqRoute;

import java.util.ArrayList;
import java.util.List;

public abstract class CollaborativeFilter extends PoolFilter {
	// ratio
	public double weight = 0; // 1 = none, 0 = all

    @Override
    public List<Arc> filter(List<Arc> pool,
                            NodeSeqRoute route,
                            DecisionProcessState state) {
        double remainingCapacity = route.getCapacity() - route.getDemand();

        Instance i = state.getInstance();

        List<Arc> filtered = new ArrayList<>();
        for (Arc candidate : pool) {
            if (state.getTaskRemainingDemandFrac(candidate) < 1.0) {
                // ie: vehicles can determine the remaining fraction from what's already been served
                if (this.getKnownDemand(candidate, i, state) <= remainingCapacity)
                    filtered.add(candidate);
            } else if ((candidate.getExpectedDemand() * weight) <= remainingCapacity)
                filtered.add(candidate);
        }

        /**
         * This is an exceptionally rare occurrence: a task has been micro-served by a vehicle, making the task's
         * exact demand known. If the remaining demand is > max capacity, the simulation will enter an infinite
         * loop, always not selecting the task due to the above filter.
         */
        int overSizeCount = 0;
        if(filtered.isEmpty() && !pool.isEmpty()) {
            for(Arc a: pool) {
                if(i.getActDemand(a) > route.getCapacity()) overSizeCount++;
            }
        }

        if(pool.size() == overSizeCount) return pool;
        else return filtered;
    }

    public abstract double getKnownDemand(Arc task, Instance i, DecisionProcessState s);

	/**
	 Called only by ReactiveGPHHProblem on initial setup.
	 */
	public void setWeight(double weight){
		this.weight = weight;
	}
}
