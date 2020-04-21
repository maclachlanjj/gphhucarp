package gputils.ManualRuns;

import ec.gp.GPTree;
import ec.multiobjective.MultiObjectiveFitness;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.poolfilter.collaborative.CollaborativePoolFilter_estimated;
import gphhucarp.decisionprocess.routingpolicy.VehicleEvaluator_RoutingPolicy;
import gphhucarp.gp.UCARPPrimitiveSet;
import gputils.LispUtils;

/**
 * Manual run for Vehicle Evaluator
 */
public class ManualRun_VE extends ManualRun{
    private static String policyA = "(+ (+ CFH RQ1) (max (* (/ (+ DC CFH) (+ CFD DC)) (min (+ FUT DEM) CTD)) (min (/ DC CTT1) (/ RQ1 (+ (max CR CFH) (max FULL CFH))))))";
    private static double individualFitness0 = 337.0039454;
    private static double individualFitness1 = 153.307958890713;

    public static void main(String[] args) {
        if(!isManualRun) return;

        /**
         * Setup the mainPolicy
         */
        String mainPolicyString = LispUtils.simplifyExpression(policyA);
        MultiObjectiveFitness mainFit = new MultiObjectiveFitness();
        mainFit.objectives = new double[2];
        mainFit.objectives[0] = individualFitness0; // manual input from the original test file
        mainFit.objectives[1] = individualFitness1;

        PoolFilter mainFilter = new CollaborativePoolFilter_estimated();

        GPTree[] trees = new GPTree[1];
        trees[0] = LispUtils.parseExpression(mainPolicyString,
                UCARPPrimitiveSet.wholePrimitiveSet());

        /**
         * Must change this to make it specific.
         */
        mainPolicy = new VehicleEvaluator_RoutingPolicy(mainFilter,
                trees);

        continueManualRun(args, mainPolicyString, mainFit);
    }





}
