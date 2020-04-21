package gphhucarp.gp.evaluation;

import ec.EvolutionState;
import ec.Fitness;
import ec.multiobjective.MultiObjectiveFitness;
import gphhucarp.core.InstanceSamples;
import gphhucarp.core.Objective;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.collaborative.CollaborativeDecisionProcess;
import gphhucarp.representation.Solution;
import gphhucarp.representation.route.NodeSeqRoute;
import gphhucarp.representation.route.TaskSeqRoute;
import gputils.ManualRuns.ManualRun;
import gputils.ManualRuns.ManualRun_VE;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A reactive evaluation model is a set of reactive decision process.
 * It evaluates a reactive routing policy by applying the policy on each decision process,
 * and returning the average normalised objective values across the processes.
 *
 * It includes
 *  - A list of reactive decision processes.
 *  - The reference objective value map, indicating the reference value
 *    of a given reactive decision process and a given objective.
 *
 * Created by gphhucarp on 31/08/17.
 */
public class CollaborativeEvaluationModel extends EvaluationModel {

    /**
     * Calculate the objective reference values.
     */
    public void calcObjRefValueMap() {
        int index = 0;
        for (InstanceSamples iSamples : instanceSamples) {
            for (long seed : iSamples.getSeeds()) {
                // create a new reactive decision process from the based intance and the seed.
                CollaborativeDecisionProcess dp =
                        CollaborativeDecisionProcess.initCollaborative(iSamples.getBaseInstance(),
                                seed, Objective.refReactiveRoutingPolicy());

                // get the objective reference values by applying the reference routing policy.
                dp.run();
                Solution<NodeSeqRoute> solution = dp.getState().getSolution();
                for (Objective objective : objectives) {
                    double objValue = solution.objValue(objective);
                    objRefValueMap.put(Pair.of(index, objective), objValue);
                    index ++;
                }
                dp.reset();
            }
        }
    }

    @Override
    public void evaluate(RoutingPolicy policy, Solution<TaskSeqRoute> plan,
                         Fitness fitness, EvolutionState state) {
        worker(policy, plan, fitness, state);
    }

    @Override
    public void evaluateOriginal(RoutingPolicy policy,
                                 Solution<TaskSeqRoute> plan,
                                 Fitness fitness, EvolutionState state) {
        worker(policy, plan, fitness, state);
    }

    public static String s = "";
    private static ArrayList<Integer> reserveList = new ArrayList<Integer>(Arrays.asList());

    private void worker(RoutingPolicy policy, Solution<TaskSeqRoute> plan, Fitness fitness, EvolutionState state){
        double[] fitnesses = new double[objectives.size()];

        int numdps = 0;
        int instance = 0;
        for (InstanceSamples iSamples : instanceSamples) {
            for (long seed : iSamples.getSeeds()) {
                // create a new reactive decision process from the based instance and the seed.
                CollaborativeDecisionProcess dp =
                        CollaborativeDecisionProcess.initCollaborative(iSamples.getBaseInstance(),
                                seed, policy);

                dp.run();
                Solution<NodeSeqRoute> solution = dp.getState().getSolution();

//                int numFracs = 0;
//                for(NodeSeqRoute route: solution.getRoutes())
//                    for(double d: route.getFracSequence())
//                        if (d < 1.0 && d > 0.0) numFracs++;
//
//                if(numFracs > 2) {
//                    s += ", " + reserve;
//                    String temp = solution.toString();
//                    int count = 0;
//                    Scanner scan = new Scanner(temp);
//                    while(scan.hasNext()) if(scan.next().equals("->")) count++;
//                    System.out.println("Reserve: " + reserve + " Count: " + count);
//                    System.out.println(solution.toString());
//                }
//                reserve++;

                if(solution.isComplete(iSamples)){
                    if(ManualRun.isManualRun && !ManualRun.smallRun){
                        System.out.println("##### FEASIBLE SOLUTION " + instance++ + " #####");
                        for (NodeSeqRoute route : solution.getRoutes())
                            System.out.println("\t\t" + route.toString());
                    }
                } else {
                    System.out.println("##############################\n@@@@@ INFEASIBLE SOLUTION GENERATED @@@@@\n##############################");
                    if(ManualRun.isManualRun) {
                        for (NodeSeqRoute route : solution.getRoutes())
                            System.out.println("\t\t" + route.toString());
                    }
                    System.exit(0);
                }

                for (int j = 0; j < fitnesses.length; j++) {
                    Objective objective = objectives.get(j);
                    double normObjValue =
                            solution.objValue(objective);

                    if(ManualRun_VE.isManualRun)
                        System.out.println("\t\tFitness " + j + ": " + normObjValue);

                    fitnesses[j] += normObjValue;
                }
                dp.reset();

                numdps ++;
            }
        }

        for (int j = 0; j < fitnesses.length; j++)
            fitnesses[j] /= numdps;

        MultiObjectiveFitness f = (MultiObjectiveFitness)fitness;
        f.setObjectives(state, fitnesses);
    }
}
