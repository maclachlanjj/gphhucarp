package gputils.ManualRuns;

import ec.gp.GPTree;
import ec.multiobjective.MultiObjectiveFitness;
import gphhucarp.algorithm.pilotsearch2.SeqPilotSearcher;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.TieBreaker;
import gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter;
import gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker;
import gphhucarp.gp.UCARPPrimitiveSet;
import gputils.LispUtils;

/**
 * Manual run for Pilot Search
 */
public class ManualRun_PS extends ManualRun{
    private static String policyA = "(+ (/ (- (/ CFD CTT1) (- CTD (* 0.6549225086608483 RQ1))) (+ SC CFH)) (/ (* (- (+ (max CFH 0.8101191605872822) CFD) (max CFD (* 0.569605979793509 CTD))) (min CFD DC)) (max FRT FUT)))";
    private static double individualFitness0 = 330.764757445205;

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
        mainPolicy = new SeqPilotSearcher(mainFilter,
                trees);

        continueManualRun(args, mainPolicyString, mainFit);
    }





}
