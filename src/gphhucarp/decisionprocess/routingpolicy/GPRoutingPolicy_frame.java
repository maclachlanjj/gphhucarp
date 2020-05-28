package gphhucarp.decisionprocess.routingpolicy;

import ec.EvolutionState;
import ec.gp.GPTree;
import ec.util.Parameter;
import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.gp.CalcPriorityProblem;
import gphhucarp.representation.route.NodeSeqRoute;
import gputils.DoubleData;

import java.util.List;

/**
 * A wrapper class to allow for clearer definition of more RoutingPolicies
 *
 * By Jordan MacLachlan on 26 Nov 2019
 */
public abstract class GPRoutingPolicy_frame extends RoutingPolicy {
    public static final String P_RECORDDATA = "record-data";
    public double numDecisions = 0.0;
    public double sumDecisionTime = 0.0;

    public static GPRoutingPolicy_frame secondaryPolicy;

    private GPTree[] gpTrees;

    public GPRoutingPolicy_frame(PoolFilter poolFilter, GPTree[] gpTrees) {
        super(poolFilter);
        name = "\"GPRoutingPolicy\"";
        this.gpTrees = gpTrees;
    }

    @Override
    public Object clone() {
        return null;
    }

    public abstract void setup(final EvolutionState state, final Parameter base);

    public GPRoutingPolicy_frame() {}

    public abstract GPRoutingPolicy_frame newTree(PoolFilter poolFilter, GPTree[] gpTree);

    public abstract boolean recordingData();

    public GPTree[] getGPTrees() {
        return gpTrees;
    }

    public void setGPTree(GPTree[] gpTrees) {
        this.gpTrees = gpTrees;
    }

    public void setSecondaryPolicy(GPRoutingPolicy_frame sec){
        secondaryPolicy = sec;
    }

    @Override
    public double priority(List<Arc> chain, NodeSeqRoute route, DecisionProcessState state) {
        CalcPriorityProblem calcPrioProb =
                new CalcPriorityProblem(chain, route, state);

        DoubleData tmp = new DoubleData();
        gpTrees[0].child.eval(null, 0, tmp, null, null, calcPrioProb);

        return tmp.value;
    }
}
