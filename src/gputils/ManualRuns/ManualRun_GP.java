package gputils.ManualRuns;

import ec.gp.GPTree;
import ec.multiobjective.MultiObjectiveFitness;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.TieBreaker;
import gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker;
import gphhucarp.gp.UCARPPrimitiveSet;
import gputils.LispUtils;

/**
 * Manual run for standard reactive GP
 */
public class ManualRun_GP extends ManualRun{
    private static String policyA = "(+ (min (- DC FRT) CFH) (max (min (/ (- DC FRT) (+ CTD RQ)) (/ 0.11144301662719736 RQ)) (* (max (* (- DC FRT) (- (* CFH CTD) (+ FUT CFD))) (/ CFR1 CTD)) (max (+ (* (min RQ1 CR) (+ CTD RQ)) (- DC FRT)) (+ CTD RQ)))))";
    private static double individualFitness0 = 335.206471;

    public static void main(String[] args) {
        if(!isManualRun) return;

        /**
         * Setup the mainPolicy
         */
        String mainPolicyString = LispUtils.simplifyExpression(policyA);
        MultiObjectiveFitness mainFit = new MultiObjectiveFitness();
        mainFit.objectives = new double[1];
        mainFit.objectives[0] = individualFitness0; // manual input from the original test file

        PoolFilter mainFilter = new ExpFeasibleNoRefillPoolFilter();

        GPTree[] trees = new GPTree[1];
        trees[0] = LispUtils.parseExpression(mainPolicyString,
                UCARPPrimitiveSet.wholePrimitiveSet());

        /**
         * Must change this to make it specific.
         */
        mainPolicy = new GPRoutingPolicy(mainFilter,
                trees);

        continueManualRun(args, mainPolicyString, mainFit);
    }





}
