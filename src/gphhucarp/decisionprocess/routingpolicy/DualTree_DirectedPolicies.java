package gphhucarp.decisionprocess.routingpolicy;

import ec.EvolutionState;
import ec.gp.GPTree;
import ec.util.Parameter;
import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.gp.CalcPriorityProblem;
import gphhucarp.representation.route.NodeSeqRoute;
import gputils.DoubleData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DualTree_DirectedPolicies extends DualTree_GPRoutingPolicy {

    public DualTree_DirectedPolicies(){ super();}

    public DualTree_DirectedPolicies(PoolFilter poolFilter, GPTree[] gpTrees) {
        super(poolFilter, gpTrees);
    }

    @Override
    public GPRoutingPolicy_frame newTree(PoolFilter poolFilter, GPTree[] gpTrees) {
        return new DualTree_DirectedPolicies(poolFilter, gpTrees);
    }

    @Override
    public void setup(EvolutionState state, Parameter base) {

    }

    @Override
    public Arc next(ReactiveDecisionSituation rds, DecisionProcess dp) {
        List<Arc> pool = rds.getPool();
        NodeSeqRoute route = rds.getRoute();
        DecisionProcessState state = rds.getState();

        List<Arc> filteredPool = poolFilter.filter(pool, route, state);

        if (filteredPool.isEmpty())
            return null;

        GPTree[] trees = getGPTrees();

        CalcPriorityProblem calcPrioProb;

        DoubleData tmpData0 = new DoubleData();
        DoubleData tmpData1 = new DoubleData();

        List<Double> tree0Vals = new ArrayList<>();
        List<Double> tree1Vals = new ArrayList<>();

        for(int i = 0; i < filteredPool.size(); i++){
            Arc tmp = filteredPool.get(i);

            calcPrioProb =
                    new CalcPriorityProblem(tmp, route, state);

            trees[0].child.eval(null, 0, tmpData0, null, null, calcPrioProb);
            trees[1].child.eval(null, 0, tmpData1, null, null, calcPrioProb);

            tree0Vals.add(tmpData0.value);
            tree1Vals.add(tmpData1.value);
        }

        List<Double> actPriorities = new ArrayList<>();

        double min0 = Collections.min(tree0Vals);
        double max0 = Collections.max(tree0Vals);

        double min1 = Collections.min(tree1Vals);
        double max1 = Collections.max(tree1Vals);

        // normalise each tree's priority on the other values of the candidate task set
        for(int i = 0; i < filteredPool.size(); i++){
            // if max0/1 == min0/1, the denominator will be NaN, but that's okay - it makes the priority
            // of the function worse (larger) as NaN > 1
            double norm0 = (tree0Vals.get(i) - min0) / (max0 - min0);
            double norm1 = (tree1Vals.get(i) - min1) / (max1 - min1);

            double priority = (norm0 + norm1) / 2;

            actPriorities.add(priority);
        }

        // select the best normalised instance
        double best = Collections.min(actPriorities);
        int index = actPriorities.indexOf(best);
        return filteredPool.get(index);
    }
}
