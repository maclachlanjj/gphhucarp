package gphhucarp.decisionprocess.poolfilter.collaborative;

import gphhucarp.core.Arc;
import gphhucarp.core.Graph;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.representation.route.NodeSeqRoute;

import java.util.ArrayList;
import java.util.List;

/**
    Merges the collaborative filter with the ExpFeasibleNoRefillPoolFilter (Ai2018 paper)
 */
public class CollaborativePoolFilter_ExpFeasible extends CollaborativeFilter {
    // merges CollaborativeFilter and ExpFeasibleNoRefillPoolFilter
    @Override
    public List<Arc> filter(List<Arc> pool,
                            NodeSeqRoute route,
                            DecisionProcessState state) {
        double remainingCapacity = route.getCapacity() - route.getDemand();

        Instance i = state.getInstance();
        Graph graph = i.getGraph();
        int currNode = route.currNode();
        int depot = i.getDepot();

        List<Arc> filtered = new ArrayList<>();
        for (Arc candidate : pool) {
            if (state.getTaskRemainingDemandFrac(candidate) < 1.0){
                if (this.getKnownDemand(candidate,i, state) <= remainingCapacity) {
                    if (currNode != depot && graph.getEstDistance(currNode, candidate.getFrom()) ==
                            graph.getEstDistance(currNode, depot) +
                                    graph.getEstDistance(depot, candidate.getFrom()))
                        continue;

                    filtered.add(candidate);
                }
            }
            else if ((candidate.getExpectedDemand() * weight) <= remainingCapacity){
                if (currNode != depot && graph.getEstDistance(currNode, candidate.getFrom()) ==
                        graph.getEstDistance(currNode, depot) +
                                graph.getEstDistance(depot, candidate.getFrom()))
                    continue;

                filtered.add(candidate);
            }
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

    @Override
    public double getKnownDemand(Arc task, Instance i, DecisionProcessState s){
        double mu = task.getExpectedDemand();
        double a = i.getActDemand(task) * (1 - s.getTaskRemainingDemandFrac(task)); // what has already been served

        double newMu = mu - a;

        return newMu;
    }
}
