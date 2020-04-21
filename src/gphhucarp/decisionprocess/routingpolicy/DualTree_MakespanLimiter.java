package gphhucarp.decisionprocess.routingpolicy;

import ec.EvolutionState;
import ec.gp.GPTree;
import ec.util.Parameter;
import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.switches.CanStopSwitch;
import gphhucarp.gp.CalcPriorityProblem;
import gphhucarp.representation.route.NodeSeqRoute;
import gputils.DoubleData;

import java.util.HashSet;
import java.util.List;

public class DualTree_MakespanLimiter extends DualTree_GPRoutingPolicy {
    public static final String P_DUALTREE_RETURNEARLY = "dual-tree-return-early";
    public static final String P_DUALTREE_ALLOWSTOPPING = "dual-tree-allow-stopping";

    private HashSet<NodeSeqRoute> decidedToStop;
    public static boolean returnEarly;
    private static boolean allowStopping;

    public DualTree_MakespanLimiter(){ super();}

    public DualTree_MakespanLimiter(PoolFilter poolFilter, GPTree[] gpTrees) {
        super(poolFilter, gpTrees);
        decidedToStop = new HashSet<>();
    }

    @Override
    public GPRoutingPolicy_frame newTree(PoolFilter poolFilter, GPTree[] gpTrees) {
        return new DualTree_MakespanLimiter(poolFilter, gpTrees);
    }

    public void setup(final EvolutionState state, final Parameter base){
        Parameter p = base.push(P_RECORDDATA);
        recordData = state.parameters.getBoolean(p, null, false);

        p = base.push(P_DUALTREE_RETURNEARLY);
        returnEarly = state.parameters.getBoolean(p, null, false);

        p = base.push(P_DUALTREE_ALLOWSTOPPING);
        allowStopping = state.parameters.getBoolean(p, null, false);
    }

    @Override
    public Arc next(ReactiveDecisionSituation rds, DecisionProcess dp) {
        DecisionProcessState state = dp.getState();
        NodeSeqRoute route = rds.getRoute();

        CalcPriorityProblem cpp;
        DoubleData tmp;

        List<Arc> candidates = poolFilter.filter(rds.getPool(), route, state);
        GPTree primaryPolicy = getGPTrees()[1];

        int vote = 0;
        for (Arc a : candidates) {
            cpp = new CalcPriorityProblem(a, route, state);
            tmp = new DoubleData();
            primaryPolicy.child.eval(null, 0, tmp, null, null, cpp);

            int res = Double.compare(tmp.value, 0);
            if (res > 0) // if policy val > 0
                vote--;
            else if (res < 0) // if policy val < 0
                vote++;
            // if policy val was exactly zero, don't change the vote.
        }

        /**
         * If the other vehicle's can feasibly solve the remaining demand, leave them to do so. Otherwise,
         * leave the depot and continue to serve the remaining tasks.
         */
        boolean feasibleStop = CanStopSwitch.canStop(rds, dp, decidedToStop);

                              // user param  // logic gate
        boolean canDoStopping = allowStopping && feasibleStop && (decidedToStop.contains(route) || (route.currNode() == state.getInstance().getDepot() && vote < 0));
                            // user param
        boolean canDoReturn = returnEarly && (vote < 0 && route.currNode() != state.getInstance().getDepot());

        // if the tree's vote was less than zero, we return to the depot and stay there.
        if (canDoStopping){
            // stop and stay at the depot.
            decidedToStop.add(route);
            return null;
        } else if(canDoReturn) {
            // return to the depot early.
            return state.getInstance().getDepotLoop();
        }
        // else, utilise a secondary policy.
        return secondaryPolicy.next(rds, dp);
    }
}
