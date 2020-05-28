package gphhucarp.gp.evaluation;

import ec.EvolutionState;
import ec.Fitness;
import ec.multiobjective.MultiObjectiveFitness;
import gphhucarp.decisionprocess.routingpolicy.DualTree_MakespanLimiter;
import gphhucarp.core.InstanceSamples;
import gphhucarp.core.Objective;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionProcess;
import gphhucarp.decisionprocess.routingpolicy.GPRolloutRoutingPolicy;
import gphhucarp.representation.Solution;
import gphhucarp.representation.route.NodeSeqRoute;
import gphhucarp.representation.route.TaskSeqRoute;
import gputils.FileOutput.DualTree_FileOutput;
import gputils.FileOutput.FileOutput;
import gputils.FileOutput.Rollout_FileOutput;
import gputils.ManualRuns.ManualRun;
import gputils.ManualRuns.ManualRun_ME;

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
public class ReactiveEvaluationModel extends EvaluationModel {

    @Override
    public void evaluate(RoutingPolicy policy, Solution<TaskSeqRoute> plan,
                                  Fitness fitness, EvolutionState state) {
        double[] fitnesses = new double[objectives.size()];

        int numdps = 0;
        for (InstanceSamples iSamples : instanceSamples) {
            for (long seed : iSamples.getSeeds()) {
                // create a new reactive decision process from the based intance and the seed.
                ReactiveDecisionProcess dp =
                        DecisionProcess.initReactive(iSamples.getBaseInstance(),
                                seed, policy);

                dp.run();
                Solution<NodeSeqRoute> solution = dp.getState().getSolution();

//                System.out.println(solution.toString());

                String s = ""; // output for recording rollout data

                if(solution.isComplete(iSamples)){
                    if(ManualRun.isManualRun){
                        System.out.println("##### FEASIBLE SOLUTION #####");
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
                            solution.objValue(objective); // getObjRefValue(i, objective);

                    if(ManualRun_ME.isManualRun)
                        System.out.println("\t\tFitness " + j + ": " + normObjValue);

                    if(DualTree_MakespanLimiter.recordData){
                        FileOutput.setup();
                        FileOutput.typeToLog(DualTree_FileOutput.saveSolution(dp.getState(), solution, normObjValue));
                        FileOutput.finish();
                    }

                    fitnesses[j] += normObjValue;
                    s += fitnesses[j];
                }

                if(GPRolloutRoutingPolicy.recordData) {
                    Rollout_FileOutput.setup();
                    for (NodeSeqRoute route : solution.getRoutes())
                        s += ", " + route.getNumActiveRefills();
                    Rollout_FileOutput.typeToLog(s);
                    Rollout_FileOutput.finish();
                }

                dp.reset();
                numdps ++;
            }
        }

        for (int j = 0; j < fitnesses.length; j++) {
            fitnesses[j] /= numdps;
        }

        MultiObjectiveFitness f = (MultiObjectiveFitness)fitness;
        f.setObjectives(state, fitnesses);
    }

    @Override
    public void evaluateOriginal(RoutingPolicy policy,
                                 Solution<TaskSeqRoute> plan,
                                 Fitness fitness, EvolutionState state) {
        double[] fitnesses = new double[objectives.size()];

        int numdps = 0;
        int instance = 0;
        for (InstanceSamples iSamples : instanceSamples) {
            for (long seed : iSamples.getSeeds()) {
                // create a new reactive decision process from the based intance and the seed.
                ReactiveDecisionProcess dp =
                        DecisionProcess.initReactive(iSamples.getBaseInstance(),
                                seed, policy);

                dp.run();
                Solution<NodeSeqRoute> solution = dp.getState().getSolution();
                for (int j = 0; j < fitnesses.length; j++) {
                    Objective objective = objectives.get(j);
                    double normObjValue =
                            solution.objValue(objective);
                    if(!solution.isComplete(iSamples)) {
                        System.out.println("infeasible");
                        normObjValue = Integer.MAX_VALUE;
                    }else if(ManualRun.isManualRun){
                        if(!ManualRun.smallRun) {
                            System.out.println("##### FEASIBLE SOLUTION: " + instance++ + " #####");
                            for (NodeSeqRoute route : solution.getRoutes())
                                System.out.println("\t\t" + route.toString());
                        }
                        System.out.println("\t\tFitness: " + normObjValue);
                    }

                    if(DualTree_MakespanLimiter.recordData){
                        FileOutput.setup();
                        FileOutput.typeToLog(DualTree_FileOutput.saveSolution(dp.getState(), solution, normObjValue));
                        FileOutput.finish();
                    }

                    fitnesses[j] += normObjValue;
                }
                dp.reset();

                numdps ++;
            }
        }

        for (int j = 0; j < fitnesses.length; j++) {
            fitnesses[j] /= numdps;
        }

        MultiObjectiveFitness f = (MultiObjectiveFitness)fitness;
        f.setObjectives(state, fitnesses);
    }

}
