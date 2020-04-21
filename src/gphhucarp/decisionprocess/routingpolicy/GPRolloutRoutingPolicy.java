package gphhucarp.decisionprocess.routingpolicy;

import ec.EvolutionState;
import ec.gp.GPTree;
import ec.util.Parameter;
import gphhucarp.core.Arc;
import gphhucarp.core.Objective;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.poolfilter.IdentityPoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.reactive.event.ReactiveRefillEvent;
import gphhucarp.decisionprocess.reactive.event.ReactiveServingEvent;
import gphhucarp.representation.route.NodeSeqRoute;
import gputils.MinHeap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * A GP-evolved routing policy. The default
 *
 * Created by gphhucarp on 30/08/17.
 *
 * Edited by Jordan MacLachlan on 26 Nov 2019
 */
public class GPRolloutRoutingPolicy extends GPRoutingPolicy_frame {
    public static final String P_ROLLOUT_ADAPTIVEK = "rollout-adaptiveK";
    public static final String P_ROLLOUT_STEPS = "rollout-steps";
    public static final String P_ROLLOUT_DEPOTTHRESHOLD = "rollout-depotThreshold";
    public static final String P_ROLLOUT_ESTIMATEDROLLOUT ="rollout-useEstimatedValues";

    public static boolean recordData;
    private static boolean cutCandidatesSwitch;
    private static int rolloutSteps;
    private static double depotThreshold;
    private static boolean useEstimatedValues;

    public static final int k = 5;  // the number of tasks on which to perform rollout
    public static int n = 5;  // the number of times to perform a simulation on each rollout

    public GPRolloutRoutingPolicy(){ super(); }

    public GPRolloutRoutingPolicy(PoolFilter poolFilter, GPTree[] gpTree) {
        super(poolFilter, gpTree);
    }

    @Override
    public void setup(EvolutionState state, Parameter base) {
        Parameter p = base.push(P_RECORDDATA);
        recordData = state.parameters.getBoolean(p, null, false);

        p = base.push(P_ROLLOUT_ADAPTIVEK);
        cutCandidatesSwitch = state.parameters.getBoolean(p, null, false);

        p = base.push(P_ROLLOUT_STEPS);
        rolloutSteps = state.parameters.getIntWithDefault(p, null, Integer.MAX_VALUE);
        if(rolloutSteps == -1) rolloutSteps = Integer.MAX_VALUE;

        p = base.push(P_ROLLOUT_DEPOTTHRESHOLD);
        depotThreshold = state.parameters.getDoubleWithDefault(p, null, 1);

        p = base.push(P_ROLLOUT_ESTIMATEDROLLOUT);
        useEstimatedValues = state.parameters.getBoolean(p, null, false);

        if(useEstimatedValues) n = 1;
    }

    public GPRolloutRoutingPolicy newTree(PoolFilter poolFilter, GPTree[] gpTrees){
        return new GPRolloutRoutingPolicy(poolFilter, gpTrees);
    }

    public GPRolloutRoutingPolicy(GPTree[] gpTrees) {
        this(new IdentityPoolFilter(), gpTrees);
    }

    public boolean recordingData(){
        return recordData;
    }

    @Override
    public Arc next(ReactiveDecisionSituation rds, DecisionProcess dp) {
        Arc[] topKcandidates = topK(rds, k);

        if(topKcandidates == null) return null;
        int k_temp = topKcandidates.length;

        /***
         *  Start cutting down the candidates
         *  Will clearly only function with original k > 1
         */
        int newMax = trimCandidates(k_temp, topKcandidates);

        Arc[] temp = new Arc[newMax];
        for(int i = 0; i < newMax; i++)
            temp[i] = topKcandidates[i];
        topKcandidates = temp;

        /**
         * Finished cutting candidates.
         */

        k_temp = topKcandidates.length;

        NodeSeqRoute originalRoute = rds.getRoute();
        DecisionProcessState originalState = dp.getState();

        double remainingFrac = (originalRoute.getCapacity() - originalRoute.getDemand()) / originalRoute.getCapacity();
        boolean underThreshold = remainingFrac <= depotThreshold;
        boolean atDepot = rds.getRoute().currNode() == 1;

        int kMod;
        if(atDepot || !underThreshold) kMod = k_temp;
        else kMod = k_temp + 1;

        int numClones = kMod * n; // additional one for checking early refill, repeat each n times (monte carlo simulation)
        DecisionProcess[] stateClones = dp.getStateClones(rds, numClones, (useEstimatedValues) ? -1 : kMod);

        double[][] results = new double[kMod][n];

        // capture the state of the returned array
        List<Boolean> returnedCapture = new ArrayList<>(originalState.getReturned());

        int i = 0;
        for(int a = 0; a < k_temp; a++){
            Arc arc = topKcandidates[a];
            for(int run = 0; run < n; run++){
                DecisionProcess sub_dp = stateClones[i++];
                NodeSeqRoute matchingRoute = sub_dp.getState().getRoute(originalState.getReturned(), originalRoute);
                sub_dp.getEventQueue().add(new ReactiveServingEvent(originalRoute.getCost(), matchingRoute, arc));
                sub_dp.getState().removeUnassignedTasks(arc); matchingRoute.setNextTask(arc);

                sub_dp.runRollout(rolloutSteps);
                results[a][run] = sub_dp.getState().getSolution().objValue(Objective.TOTAL_COST); // how to access EvaluationModel?

                originalState.setReturned(new ArrayList<>(returnedCapture));
            }
        }

        if(!atDepot && underThreshold) {
            for (int run = 0; run < n; run++) {
                DecisionProcess sub_dp = stateClones[i++];
                NodeSeqRoute cloneRoute = sub_dp.getState().getRoute(originalState.getReturned(), originalRoute);
                sub_dp.getEventQueue().add(new ReactiveRefillEvent(originalRoute.getCost(), cloneRoute));
                sub_dp.runRollout(rolloutSteps);

                results[k_temp][run] = sub_dp.getState().getSolution().objValue(Objective.TOTAL_COST); // how to access EvaluationModel?

                originalState.setReturned(new ArrayList<>(returnedCapture));
            }
        }

        double[] scores = new double[kMod];
        double min = Double.POSITIVE_INFINITY;
        int index = -1;
        for(int j = 0; j < kMod; j++){
            double avg = 0;
            for(int np = 0; np < n; np++)
                avg += results[j][np];
            scores[j] = avg / n;
            if(min > scores[j]){
                min = scores[j];
                index = j;
            }
        }

        if(index == k_temp) {
            // an early refill is the best option
            Arc depotLoop = rds.getState().getInstance().getDepotLoop();
            originalRoute.setNextTask(depotLoop);
            originalRoute.toggleActiveRefill(false);
            return null;
        } else {
            // one of the tasks is the best pick
            originalRoute.setNextTask(topKcandidates[index]);
//            System.out.println("selecting: " + topKcandidates[index]);
            return topKcandidates[index];
        }
    }

    public Arc[] topK(ReactiveDecisionSituation rds, int k){
        List<Arc> pool = rds.getPool();
        NodeSeqRoute route = rds.getRoute();
        DecisionProcessState state = rds.getState();

        List<Arc> filteredPool = poolFilter.filter(pool, route, state);

        if (filteredPool.isEmpty())
            return null;

        int k_temp = Math.min(filteredPool.size(), k);

        MinHeap heap = new MinHeap(k_temp); // I think this will be a reasonably efficient means of getting the lowest k_temp items.

        for (int i = 0; i < filteredPool.size(); i++) {
            Arc tmp = filteredPool.get(i);
            tmp.setPriority(priority(tmp, route, state));

            heap.insert(tmp, tieBreaker);
        }

        heap.minHeap();

        Arc[] resHeap = Arrays.copyOfRange(heap.getHeap(), 1, k_temp+1); // exclude the 'root' node of the heap (a dummy arc not dissimilar to the depot loop)

        Arrays.sort(resHeap, new Comparator<Arc>() {
            @Override
            public int compare(Arc a1, Arc a2) {
                return Double.compare(a1.getPriority(), a2.getPriority());
            }
        });

        return resHeap;
    }

    public int trimCandidates(int k_temp, Arc[] topKcandidates){
        double distances[] = new double[topKcandidates.length];

        for(int i = 0; i < topKcandidates.length - 1; i++)
            distances[i] = topKcandidates[i + 1].getPriority() - topKcandidates[i].getPriority();

        double max = Double.NEGATIVE_INFINITY;
        int index = -1;

        for(int i = 0; i < distances.length; i++){
            double v = distances[i];
            if(Double.compare(v, max) > 0){
                max = v;
                index = i;
            }
        }

        // if the top task shares priority with other candidate tasks, keep them.
        int altIndex = 1;
        double firstPriority = topKcandidates[0].getPriority();
        for(int i = 1; i < distances.length; i++)
            if(Double.compare(topKcandidates[i].getPriority(), firstPriority) == 0) altIndex++;

        return Math.max(index + 1, altIndex);
    }

    public int trimCandidates2(int k_temp, Arc[] topKcandidates){
        if(! (cutCandidatesSwitch && k_temp > 1)) return k_temp;

        // first, calculate the priority-means of each set of arcs
        double[] means = new double[k_temp];

        for (int i = 0; i < k_temp; i++)
            for (int j = i; j < k_temp; j++)
                means[i] += topKcandidates[j].getPriority();

        for (int i = 0; i < k_temp; i++)
            means[i] /= (i + 1);

        double[] distMeans = new double[k_temp - 1];

        double max = Double.NEGATIVE_INFINITY;
        int maxIndex = -1;
        distMeans[0] = means[1] - means[0];
        boolean allTheSame = true;
        for (int i = 1; i < k_temp - 1; i++) {
            distMeans[i] = means[i + 1] - means[i];

            if (distMeans[i] != distMeans[0]) allTheSame = false;

            if (Double.compare(distMeans[i], max) >= 0) {
                max = distMeans[i];
                maxIndex = i;
            }
        }

        if (allTheSame) {
            return k_temp; // superfluous, but adding here for clarity: if the distMeans are all the same, include all candidates.
        } else {
            return maxIndex + 1; // otherwise, slice off the larger arcs
        }
    }

    public static boolean usingEstimatedValues(){
        return useEstimatedValues;
    }
}
