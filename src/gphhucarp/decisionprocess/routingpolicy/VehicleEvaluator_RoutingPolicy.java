package gphhucarp.decisionprocess.routingpolicy;

import ec.EvolutionState;
import ec.gp.GPTree;
import ec.util.Parameter;
import gphhucarp.core.Arc;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.collaborative.events.CollaborativeServingEvent;
import gphhucarp.decisionprocess.poolfilter.collaborative.CollaborativeFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.gp.CalcPriorityProblem;
import gphhucarp.representation.route.NodeSeqRoute;
import gputils.DoubleData;

import java.util.*;

/**
 * Most depricated code from the 2019 summer research period.
 * This formed the foundation of the Matrix Evaluator.
 */
public class VehicleEvaluator_RoutingPolicy extends GPRoutingPolicy_frame {
    // matrix of priority values (also stored independently per task)
    //
    // t0i = t0.inverse()
    // p0 = priority0
    //
    //      r0  r1  r2  r3  r4  r5  r6  r7  r8  ...
    // t0   p0  p1  ...
    // t0i  ...
    // t1
    // t1i
    // t2
    // t2i
    // t3
    // t3i
    // ...
    //
    HashMap<Arc, HashMap<NodeSeqRoute, Double>> vehPriorityMatrix;
    // relatively simple method of storing using objects as indices

    public VehicleEvaluator_RoutingPolicy(){ super(); }

    public VehicleEvaluator_RoutingPolicy(PoolFilter poolFilter, GPTree[] gpTrees){
        super(poolFilter, gpTrees);
        this.vehPriorityMatrix = new HashMap<>();
    }

    /**
     * Allocates a number of tasks to each vehicle, given the capacity constraint.
     * Given the constraint, this does not form giant routes.
     *
     * @param set
     * @param dp
     */
    public void updateMatrixV3(List<Arc> set, DecisionProcess dp){
        DecisionProcessState state = dp.getState();
        List<NodeSeqRoute> routes = state.getSolution().getRoutes();

        for(NodeSeqRoute route: routes)
            route.removeAllFromQueue(set);

        GPTree[] trees = this.getGPTrees();

        DoubleData tmpData = new DoubleData();
        CalcPriorityProblem calcPrioProb;

        // construct a set of temporary, full vehicle queues, per arc
        HashMap<Arc, PriorityQueue<RouteTuple>> tempQueues = new HashMap<>();
        for(Arc a: set){
            tempQueues.put(a, new PriorityQueue<>(new Comparator<RouteTuple>() {
                @Override
                public int compare(RouteTuple o1, RouteTuple o2) {
                    return Double.compare(o1.getPriority(), o2.getPriority());
                }
            }));

            for(NodeSeqRoute r: routes){
                if(r.hasStopped())
                    continue;

                calcPrioProb = new CalcPriorityProblem(a, r, state);
                trees[0].child.eval(null, 0, tmpData, null, null, calcPrioProb);

                tempQueues.get(a).add(new RouteTuple(r, tmpData.value));
            }
        }

        // now deconstruct the temporary queues to decide the final ordering
        CollaborativeFilter filter = (CollaborativeFilter) poolFilter;
        Instance instance = state.getInstance();

        HashSet<Arc> examined = new HashSet<>();
        List<Arc> recurse = new ArrayList<>();
        for(Arc a: set){
            if(examined.contains(a)) continue;

            Arc inv = a.getInverse();

            // one for each direction of each arc
            PriorityQueue<RouteTuple> thisQueue = tempQueues.get(a);
            PriorityQueue<RouteTuple> altQueue = tempQueues.get(inv);

            RouteTuple thisRoute = thisQueue.poll();
            RouteTuple altRoute = altQueue.poll();

            double frac = state.getTaskRemainingDemandFrac(a);
            double knownDemand = ((Double.compare(frac, 1.0) < 0) ? filter.getKnownDemand(a, instance, state) : a.getExpectedDemand());

            int res = Double.compare(thisRoute.priority, altRoute.priority);
            while(true){
                if(res < 0){
                    // let's see if this task has a higher priority than any others in this route's queue
                    // first, check whether this route can serve the task given the capacity constraint and its current queue
                    if(!canFeasiblyServe(currentQueueDemand(thisRoute.route, state), knownDemand, thisRoute.route)) {
                        List<Arc> queuedTasks = thisRoute.route.getQueuedTasks();

                        if(queuedTasks.size() > 0) {
                            Arc queuedTask = queuedTasks.get(queuedTasks.size() - 1);

                            double queuedFrac = state.getTaskRemainingDemandFrac(queuedTask);
                            double queuedKnownDemand = ((Double.compare(queuedFrac, 1.0) < 0) ? filter.getKnownDemand(queuedTask, instance, state) : queuedTask.getExpectedDemand());

                            if (Double.compare(thisRoute.priority, queuedTask.getPriority()) < 0 &&
                                    canFeasiblyServe(currentQueueDemand(thisRoute.route, state) - queuedKnownDemand, knownDemand, thisRoute.route)) {
                                recurse.add(queuedTask); recurse.add(queuedTask.getInverse());
                                thisRoute.route.removeFromQueue(queuedTask);
                                a.setPriority(thisRoute.getPriority());
                                assignTask(thisRoute.route, a, dp);
                                break;
                            }
                        }
                        // if we get here, this task isn't as important as any of the tasks in the vehicle's queue.

                        // therefore, we check the next vehicle.
                        if (thisQueue.size() > 0) {
                            thisRoute = thisQueue.poll();
                            res = Double.compare(thisRoute.priority, altRoute.priority);
                        } else {
                            if (altQueue.size() == 0) break;
                            else res = 1;
                        }
                    } else {
                        // otherwise, if the vehicle can feasibly complete the task given its current queue, we add it.
                        a.setPriority(thisRoute.priority);
                        assignTask(thisRoute.route, a, dp);
                        break;
                    }

                } else if(res > 0){ // repeat for the alternate queue
                    if(!canFeasiblyServe(currentQueueDemand(altRoute.route, state), knownDemand, altRoute.route)) {
                        List<Arc> queuedTasks = altRoute.route.getQueuedTasks();

                        if(queuedTasks.size() > 0) {
                            Arc queuedTask = queuedTasks.get(queuedTasks.size() - 1);

                            double queuedFrac = state.getTaskRemainingDemandFrac(queuedTask);
                            double queuedKnownDemand = ((Double.compare(queuedFrac, 1.0) < 0) ? filter.getKnownDemand(queuedTask, instance, state) : queuedTask.getExpectedDemand());

                            if(Double.compare(altRoute.priority, queuedTask.getPriority()) < 0 &&
                                    canFeasiblyServe(currentQueueDemand(altRoute.route, state) - queuedKnownDemand, knownDemand, altRoute.route)){
                                recurse.add(queuedTask); recurse.add(queuedTask.getInverse());
                                altRoute.route.removeFromQueue(queuedTask);
                                a.setPriority(altRoute.getPriority());
                                assignTask(altRoute.route, a, dp);
                                break;
                            }
                        }

                        if(altQueue.size() > 0) {
                            altRoute = altQueue.poll();
                            res = Double.compare(thisRoute.priority, altRoute.priority);
                        } else {
                            if(thisQueue.size() == 0) break;
                            else res = -1;
                        }
                    } else {
                        a.setPriority(altRoute.priority);
                        assignTask(altRoute.route, a, dp);
                        break;
                    }
                } else { // priorities are equal, break tie and repeat loop.
                    int thisIndex = state.getRouteAdjacencyList(a).indexOf(thisRoute.route);
                    int altIndex = state.getRouteAdjacencyList(inv).indexOf(altRoute.route);

                    res = thisIndex - altIndex;
                    if(res == 0) res = tieBreaker.breakTie(a, inv);
                }
            }

            examined.add(inv);
        }

        // recursion to repeat this process and (try) re-assign tasks that have been replaced in queues.
        if(recurse.size() > 0) {
            updateMatrixV3(recurse, dp);
        }
    }

    /**
     * Allocates a number of tasks to each vehicle, given the capacity constraint.
     * Given the constraint, this does not form giant routes.
     *
     * @param set
     * @param dp
     */
    public void updateMatrixV2(List<Arc> set, DecisionProcess dp){
        DecisionProcessState state = dp.getState();
        List<NodeSeqRoute> routes = state.getSolution().getRoutes();

        for(NodeSeqRoute route: routes)
            route.removeAllFromQueue(set);

        GPTree[] trees = this.getGPTrees();

        DoubleData tmpData = new DoubleData();
        CalcPriorityProblem calcPrioProb;

        // construct a set of temporary, full vehicle queues, per arc
        HashMap<Arc, PriorityQueue<RouteTuple>> tempQueues = new HashMap<>();
        for(Arc a: set){
            tempQueues.put(a, new PriorityQueue<>(new Comparator<RouteTuple>() {
                @Override
                public int compare(RouteTuple o1, RouteTuple o2) {
                    return Double.compare(o1.getPriority(), o2.getPriority());
                }
            }));

            for(NodeSeqRoute r: routes){
                if(r.hasStopped())
                    continue;

                calcPrioProb = new CalcPriorityProblem(a, r, state);
                trees[0].child.eval(null, 0, tmpData, null, null, calcPrioProb);

                tempQueues.get(a).add(new RouteTuple(r, tmpData.value));
            }
        }

        // now deconstruct the temporary queues to decide the final ordering

        CollaborativeFilter filter = (CollaborativeFilter) poolFilter;
        Instance instance = state.getInstance();

//        System.out.println("New method call");
        HashSet<Arc> examined = new HashSet<>();
        for(Arc a: set){
            if(examined.contains(a)) continue;

            Arc inv = a.getInverse();

            PriorityQueue<RouteTuple> thisQueue = tempQueues.get(a);
            PriorityQueue<RouteTuple> altQueue = tempQueues.get(inv);

            RouteTuple thisRoute = thisQueue.poll();
            RouteTuple altRoute = altQueue.poll();

            double frac = state.getTaskRemainingDemandFrac(a);
            double knownDemand = ((Double.compare(frac, 1.0) < 0) ? filter.getKnownDemand(a, instance, state) : a.getExpectedDemand());

            int res = Double.compare(thisRoute.priority, altRoute.priority);
//            System.out.println("\t------");
            while(true){
                if(res < 0){
                    if(canFeasiblyServe(currentQueueDemand(thisRoute.route, state), knownDemand, thisRoute.route)) {
                        a.setPriority(thisRoute.priority);
//                        System.out.println("\t\tAdding to: " + thisRoute.route.hashCode());
                        thisRoute.route.addTaskToQueue(a);
                        break;
                    } else {
                        if(thisQueue.size() > 0) {
                            thisRoute = thisQueue.poll();
                            res = Double.compare(thisRoute.priority, altRoute.priority);
                        } else {
                            if(altQueue.size() == 0) break;
                            else res = 1;
                        }
                    }
                } else if(res > 0){
                    if(canFeasiblyServe(currentQueueDemand(altRoute.route, state), knownDemand, altRoute.route)){
                        inv.setPriority(altRoute.priority);
//                        System.out.println("\t\tAdding to: " + altRoute.route.hashCode());
                        altRoute.route.addTaskToQueue(inv);
                        break;
                    } else {
                        if(altQueue.size() > 0) {
                            altRoute = altQueue.poll();
                            res = Double.compare(thisRoute.priority, altRoute.priority);
                        } else {
                            if(thisQueue.size() == 0) break;
                            else res = -1;
                        }
                    }
                } else {
                    int thisIndex = state.getRouteAdjacencyList(a).indexOf(thisRoute.route);
                    int altIndex = state.getRouteAdjacencyList(inv).indexOf(altRoute.route);

                    res = thisIndex - altIndex;
                    if(res == 0) res = tieBreaker.breakTie(a, inv);
//                    System.out.println("\tres: " + res);
                }
            }

            examined.add(inv);
        }

//        System.out.println("ROUTES:");
//        for(NodeSeqRoute r: routes){
//            System.out.println("\troute queue length: " + r.getQueuedTasks().size());
//        }
    }

    /**
     * Original Method.
     *
     * Forms single, giant routes.
     *
     * @param set
     * @param dp
     */
    public void updateMatrix(List<Arc> set, DecisionProcess dp){
        DecisionProcessState state = dp.getState();
        List<NodeSeqRoute> routes = state.getSolution().getRoutes();

        for(NodeSeqRoute route: routes)
            route.clearTaskQueue();
//            route.removeAllFromQueue(set);

        GPTree[] trees = this.getGPTrees();

        DoubleData tmpData = new DoubleData();
        CalcPriorityProblem calcPrioProb;

        double minRoutePriority;
        NodeSeqRoute minRoute;
        for(Arc a: set) {
            minRoutePriority = Double.MAX_VALUE;
            minRoute = null;
            for (NodeSeqRoute r : routes) {
                if(r.hasStopped())
                    continue;

                HashMap<NodeSeqRoute, Double> internal = new HashMap<>();
                calcPrioProb = new CalcPriorityProblem(a, r, state);
                trees[0].child.eval(null, 0, tmpData, null, null, calcPrioProb);

                internal.put(r, tmpData.value);
                vehPriorityMatrix.put(a, internal);

                if(Double.compare(tmpData.value, minRoutePriority) < 0 || minRoute == null){
                    minRoute = r;
                    minRoutePriority = tmpData.value;
                }

                internal.put(r, tmpData.value);
                vehPriorityMatrix.put(a, internal);
            }
            a.setPriority(minRoutePriority);    // stored in the task simply to save redevelopment time
            a.setMinRoute(minRoute);
        }

        HashSet<Arc> complete = new HashSet<>();
        for(Arc a: set){
            if(complete.contains(a)) continue;

            Arc inv = a.getInverse();
            double priority = a.getPriority();
            double invPriority = inv.getPriority();

            int res = Double.compare(invPriority, priority);
            if(res == 0) res = tieBreaker.breakTie(a, inv);

            NodeSeqRoute route;
            if (res < 0) {
                route = inv.getMinRoute();
                route.addTaskToQueue(inv);
//                if(route.hasStopped()) { route.restart(); dp.getEventQueue().add(new CollaborativeServingEvent(route.getCost(), route, inv)); }
            }
            else {
                route = a.getMinRoute();
                route.addTaskToQueue(a);
//                if(route.hasStopped()) { route.restart(); dp.getEventQueue().add(new CollaborativeServingEvent(route.getCost(), route, a)); }
            }

            complete.add(a);
            complete.add(inv);
        }

//        System.out.println("##### NEW EVALUATION #####");
//        for(NodeSeqRoute route: routes){
//            System.out.println("Route");
//            for(Arc a: route.getQueuedTasks()){
//                System.out.println("\tfrom: " + a.getFrom() + " to: " + a.getTo());
//            }
//        }
    }

    public double currentQueueDemand(NodeSeqRoute r, DecisionProcessState state){
        Instance instance = state.getInstance();
        double plannedDemand = 0;

        CollaborativeFilter filter = (CollaborativeFilter) poolFilter;

        for(Arc plannedTask: r.getQueuedTasks()){
            double remFrac = state.getTaskRemainingDemandFrac(plannedTask);
            if(Double.compare(remFrac, 1.0) < 0) plannedDemand += filter.getKnownDemand(plannedTask, instance, state);
            else plannedDemand += plannedTask.getExpectedDemand();
        }

        return plannedDemand;
    }

    /**
     * This is effectively a feasibility filter, for routes
     */
    public boolean canFeasiblyServe(double currentQueueDemand, double proposedTaskDemand, NodeSeqRoute route) {
//        System.out.println("currentQueueDemand: " + currentQueueDemand + " proposedTaskDemand: " + proposedTaskDemand);

        double remainingDemand = route.getCapacity() - route.getDemand();
        return Double.compare(remainingDemand, (currentQueueDemand + proposedTaskDemand)) >= 0;
    }

    public void clearThenUpdateMatrix(NodeSeqRoute main, List<Arc> set, DecisionProcess dp){
        main.clearTaskQueue(); // e.g. if a route fails, clear its remaining tasks, then reevaluate the other routes
        List<Arc> resSet = new ArrayList<Arc>();
        for(Arc a: set){    // we don't want to assign the same task twice
            resSet.add(a); resSet.add(a.getInverse());
        }
        updateMatrix(resSet, dp);
    }

    public void assignTask(NodeSeqRoute r, Arc a, DecisionProcess dp){
        r.addTaskToQueue(a);
        if(r.hasStopped()){
            // don't have a 'current time' measure...
            dp.getEventQueue().add(new CollaborativeServingEvent(r.getCost(), r, a));
            r.restart();
        }
    }

    @Override
    public void setup(EvolutionState state, Parameter base) {

    }

    @Override
    public GPRoutingPolicy_frame newTree(PoolFilter poolFilter, GPTree[] gpTrees) {
        return new VehicleEvaluator_RoutingPolicy(poolFilter, gpTrees);
    }

    @Override
    public boolean recordingData() {
        return false;
    }



    @Override
    public Arc next(ReactiveDecisionSituation rds, DecisionProcess dp) {
        NodeSeqRoute route = rds.getRoute();
        List<Arc> pool = rds.getPool();

        updateMatrix(dp.getState().getUnassignedTasks(), dp);

        if (route.hasNextTask()) {
            List<Arc> temp = new ArrayList<>();
            temp.add(route.peekNextTask());
            List<Arc> filtered = poolFilter.filter(pool, route, dp.getState());
            if (filtered.size() > 0)
                return route.pollNextTask();
        }

        return null;
    }

    public class RouteTuple{
        private NodeSeqRoute route;
        private double priority;

        public RouteTuple(NodeSeqRoute r, double p){
            route = r; priority = p;
        }

        public NodeSeqRoute getRoute(){
            return route;
        }

        public double getPriority(){
            return priority;
        }
    }
}
