package gphhucarp.gp;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPProblem;
import ec.gp.GPTree;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;
import gphhucarp.core.Objective;
import gphhucarp.decisionprocess.TieBreaker;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy_frame;
import gphhucarp.gp.evaluation.EvaluationModel;
import gputils.FileOutput.DualTree_FileOutput;
import gputils.LispUtils;

import java.util.List;

/**
 * A reactive GPHH problem to evaluate a reactive routing policy during the GPHH.
 * The evaluationg model is a reactive evaluation model.
 * It also includes a pool filter specifying how to filter out the pool of candidate tasks.
 */

public class ReactiveGPHHProblem extends GPProblem implements SimpleProblemForm {

    public static final String P_EVAL_MODEL = "eval-model";
    public static final String P_POOL_FILTER = "pool-filter";
    public static final String P_TIE_BREAKER = "tie-breaker";
    public static final String P_ROUTING_POLICY = "routing-policy";
    public static final String P_SECONDARY_ROUTING_POLICY = "secondary-routing-policy";

    public EvaluationModel evaluationModel;
    protected PoolFilter poolFilter;
    protected TieBreaker tieBreaker;
    protected GPRoutingPolicy_frame prototypePolicy;
    protected GPRoutingPolicy_frame secondaryPrototypePolicy;

    public List<Objective> getObjectives() {
        return evaluationModel.getObjectives();
    }

    public EvaluationModel getEvaluationModel() {
        return evaluationModel;
    }

    public PoolFilter getPoolFilter() {
        return poolFilter;
    }

    public TieBreaker getTieBreaker() {
        return tieBreaker;
    }

    public void rotateEvaluationModel() {
        evaluationModel.rotateSeeds();
    }

    @Override
    public void setup(final EvolutionState state, final Parameter base) {
        super.setup(state, base);

        Parameter p = base.push(P_EVAL_MODEL);
        evaluationModel = (EvaluationModel)(
                state.parameters.getInstanceForParameter(
                        p, null, EvaluationModel.class));
        evaluationModel.setup(state, p);

        p = base.push(P_POOL_FILTER);
        poolFilter = (PoolFilter)(
                state.parameters.getInstanceForParameter(
                        p, null, PoolFilter.class));

        p = base.push(P_TIE_BREAKER);
        tieBreaker = (TieBreaker)(
                state.parameters.getInstanceForParameter(
                        p, null, TieBreaker.class));

        p = base.push(P_ROUTING_POLICY);
        if(state.parameters.exists(p)) {
            prototypePolicy = (GPRoutingPolicy_frame) (
                    state.parameters.getInstanceForParameter(
                            p, null, GPRoutingPolicy_frame.class));

            prototypePolicy.setup(state, p);
        }

        Parameter p_old = p;
        p = base.push(P_SECONDARY_ROUTING_POLICY);
        if(state.parameters.exists(p)) {
            secondaryPrototypePolicy = (GPRoutingPolicy_frame) (
                    state.parameters.getInstanceForParameter(p, null, GPRoutingPolicy_frame.class));
            secondaryPrototypePolicy.setup(state, p_old);
        }
    }

    public GPRoutingPolicy_frame[] buildRoutingPolicies(List<String> expressions){
        boolean hasSecondaryPolicy = secondaryPrototypePolicy == null ? false : true;

        GPRoutingPolicy_frame[] res = new GPRoutingPolicy_frame[hasSecondaryPolicy ? 2 : 1];

        GPTree[] trees = new GPTree[hasSecondaryPolicy ? 2 : 1];
        trees[0] = LispUtils.parseExpression(expressions.get(0), UCARPPrimitiveSet.wholePrimitiveSet());
        if(hasSecondaryPolicy) trees[1] = LispUtils.parseExpression(expressions.get(1), UCARPPrimitiveSet.wholePrimitiveSet());

        res[0] = prototypePolicy.newTree(poolFilter, trees);
        if(hasSecondaryPolicy) res[1] = secondaryPrototypePolicy.newTree(poolFilter, trees);

        return res;
    }

    @Override
    public void evaluate(EvolutionState state,
                         Individual indi,
                         int subpopulation,
                         int threadnum) {
        GPRoutingPolicy_frame policy;

        if(prototypePolicy != null)
            policy = prototypePolicy.newTree(poolFilter, ((GPIndividual) indi).trees);
        else policy = new GPRoutingPolicy(poolFilter, ((GPIndividual)indi).trees[0]);

        // this actually only does anything if the primary policy utilises the secondary policy
        if(secondaryPrototypePolicy != null){
            GPRoutingPolicy_frame secondaryPolicy =
                    secondaryPrototypePolicy.newTree(poolFilter, ((GPIndividual)indi).trees);
            policy.setSecondaryPolicy(secondaryPolicy);
            DualTree_FileOutput.setupDataCollection(((GPIndividual)indi).trees[1]);
        }

        // the evaluation model is reactive, so no plan is specified.
        evaluationModel.evaluate(policy, null, indi.fitness, state);

        indi.evaluated = true;
    }
}
