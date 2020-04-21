package gputils.ManualRuns;

import ec.Evaluator;
import ec.EvolutionState;
import ec.Evolve;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import ec.util.ParameterDatabase;
import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy_frame;
import gphhucarp.gp.ReactiveGPHHProblem;
import gphhucarp.gp.evaluation.EvaluationModel;

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

public abstract class ManualRun extends Evolve {
    public static boolean isManualRun = false;
    public static boolean smallRun = true;

    private static String fileSuffix = "gdb1.csv";

    public static GPRoutingPolicy_frame mainPolicy;
    private static String output = "";
    private static List<Integer> hashcodes = new ArrayList<>();

    public static void continueManualRun(String[] args, String mainPolicyString, MultiObjectiveFitness mainFit){
        System.out.println("primary policy: " + mainPolicyString);

        /**
         * Setup the parameters
         */
        ParameterDatabase parameters = loadParameterDatabase(args);
        EvolutionState state = initialize(parameters, 0);

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

    public static void recordPriorities(java.util.List<Arc> pool) {
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
