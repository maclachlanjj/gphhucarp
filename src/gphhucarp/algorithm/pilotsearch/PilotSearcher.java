package gphhucarp.algorithm.pilotsearch;

import ec.gp.GPTree;
import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy_frame;

public abstract class PilotSearcher extends GPRoutingPolicy_frame {
    public PilotSearcher(){};

    public PilotSearcher(PoolFilter poolFilter, GPTree[] gpTrees){
        super(poolFilter, gpTrees);
    }

    /**
     * Select the next task of the give route based on the decision situation and policy.
     * @param rds the decision situation.
     * @param routingPolicy the routing policy.
     * @return the selected next task.
     */
    public abstract Arc next(ReactiveDecisionSituation rds,
                             RoutingPolicy routingPolicy);
}
