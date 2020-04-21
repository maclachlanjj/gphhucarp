package gphhucarp.decisionprocess.routingpolicy;

import ec.EvolutionState;
import ec.gp.GPTree;
import ec.util.Parameter;
import gphhucarp.core.Arc;
import gphhucarp.representation.route.NodeSeqRoute;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.poolfilter.IdentityPoolFilter;
import gphhucarp.gp.CalcPriorityProblem;
import gputils.DoubleData;

/**
 * A GP-evolved routing policy. The default
 *
 * Created by gphhucarp on 30/08/17.
 *
 * Edited by Jordan MacLachlan on 26 Nov 2019
 */
public class GPRoutingPolicy extends GPRoutingPolicy_frame {
    private static boolean recordData;
    public GPRoutingPolicy(){ super(); }

    public GPRoutingPolicy(PoolFilter poolFilter, GPTree[] gpTrees) {
        super(poolFilter, gpTrees);
    }

    public GPRoutingPolicy(PoolFilter poolFilter, GPTree gpTree){
        super(poolFilter, new GPTree[]{gpTree});
    }

    @Override
    public void setup(EvolutionState state, Parameter base) {
        Parameter p = base.push(P_RECORDDATA);
        recordData = state.parameters.getBoolean(p, null, false);
    }

    public GPRoutingPolicy newTree(PoolFilter poolFilter, GPTree[] gpTrees){
        return new GPRoutingPolicy(poolFilter, gpTrees);
    }

    @Override
    public boolean recordingData() {
        return false;
    }

    public GPRoutingPolicy(GPTree[] gpTrees) {
        this(new IdentityPoolFilter(), gpTrees);
    }
}
