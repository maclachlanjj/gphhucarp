package gputils.ManualRuns;

import ec.Evaluator;
import ec.EvolutionState;
import ec.Evolve;
import ec.gp.GPTree;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import ec.util.ParameterDatabase;
import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.TieBreaker;
import gphhucarp.decisionprocess.poolfilter.collaborative.CollaborativePoolFilter_estimated;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy_frame;
import gphhucarp.decisionprocess.routingpolicy.VehicleEvaluator_RoutingPolicy;
import gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker;
import gphhucarp.gp.ReactiveGPHHProblem;
import gphhucarp.gp.UCARPPrimitiveSet;
import gphhucarp.gp.evaluation.EvaluationModel;
import gputils.LispUtils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Manual Run for Dual Tree (needs some tidying up)
 */
public class ManualRun_DT extends ManualRun {
    private static String fileSuffix = "gdb1.csv";

    public static GPRoutingPolicy_frame mainPolicy;
    public static GPRoutingPolicy_frame secondaryPolicy;

    /**
     * CHANGE THE FOLLOWING TO CHANGE THE TARGET OF THE ALGORITHM
     */

    // MakespanLimiter policy: policyA = routing policy. policyB = classifier
    private static String policyA = "(+ (/ (- (/ CFD CTT1) (- CTD (* 0.6549225086608483 RQ1))) (+ SC CFH)) (/ (* (- (+ (max CFH 0.8101191605872822) CFD) (max CFD (* 0.569605979793509 CTD))) (min CFD DC)) (max FRT FUT)))";
    private static double individualFitness = 330.764757445205;

    // note this is only actually used if the first policy is of type dual-tree
    private static String policyB = "(min (/ (- DC CFR1) (* CFR1 CTT1)) (min (- DC CFR1) (+ DEM (/ DEM (* CFR1 (- DC CFR1))))))";

    public static PoolFilter mainFilter = new CollaborativePoolFilter_estimated();

    private static String output = "";
    private static List<Integer> hashcodes = new ArrayList<>();

    public static void main(String[] args) {
        if(!isManualRun) return;

        /**
         * Setup the mainPolicy
         */
        String mainPolicyString = LispUtils.simplifyExpression(policyA);
        String secondaryPolicyString = LispUtils.simplifyExpression(policyB);
        MultiObjectiveFitness mainFit = new MultiObjectiveFitness();
        mainFit.objectives = new double[1];
        mainFit.objectives[0] = individualFitness; // manual input from the original test file

        System.out.println("primary policy: " + mainPolicyString);
        System.out.println("secondary policy: " + secondaryPolicyString);

        GPTree[] trees = new GPTree[2];
        trees[0] = LispUtils.parseExpression(mainPolicyString,
                UCARPPrimitiveSet.wholePrimitiveSet());
        trees[1] = LispUtils.parseExpression(secondaryPolicyString,
                UCARPPrimitiveSet.wholePrimitiveSet());

        /**
         * Must change this to make it specific.
         */
        mainPolicy = new VehicleEvaluator_RoutingPolicy(mainFilter,
                trees);

        secondaryPolicy = new GPRoutingPolicy(mainFilter,
                new GPTree[]{trees[0]});

        mainPolicy.setSecondaryPolicy(secondaryPolicy);

        /**
         * Setup the parameters
         */
        ParameterDatabase parameters = Evolve.loadParameterDatabase(args);
        EvolutionState state = Evolve.initialize(parameters, 0);

        Parameter p;

        // setup the evaluator, essentially the test evaluation model
        p = new Parameter(EvolutionState.P_EVALUATOR);
        state.evaluator = (Evaluator)
                (parameters.getInstanceForParameter(p, null, Evaluator.class));
        state.evaluator.setup(state, p);

        // the fields for testing
        ReactiveGPHHProblem testProblem = (ReactiveGPHHProblem)state.evaluator.p_problem;
        EvaluationModel testEvaluationModel = testProblem.evaluationModel;

        /**
         * Run the simulation
         */
        testEvaluationModel.evaluateOriginal(mainPolicy,null, mainFit, state);

        outputString();
    }

    public static void recordPriorities(List<Arc> pool) {
        if (!isManualRun) return;

        List<Arc> temp = new ArrayList<Arc>();
        for(Arc a: pool) temp.add(a);

        Collections.sort(temp, new Comparator<Arc>(){
            @Override
            public int compare(Arc a1, Arc a2) {
                if (a1.getPriority() > a2.getPriority())
                    return -1;
                if (a1.getPriority() < a2.getPriority())
                    return 1;
                return 0;
            }
        });

        hashcodes = new ArrayList<>();

        for(Arc a: temp)
            output += a.hashCode() + ", ";

        output += "\n";
    }

    public static String s = "";
    private static void outputString(){
        if(!isManualRun) return;

        BufferedWriter writer = null;
        try {
            String outputDir = "C:\\Users\\Jorda\\Documents\\Uni\\gphhucarp\\";
            writer = new BufferedWriter(new FileWriter((new File(outputDir + "manualRun_" + fileSuffix))));

            writer.write(output);
            writer.close();
        }
        catch(IOException e) { e.printStackTrace(); }

        StringSelection stringSelection = new StringSelection(s);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }
}
