package gphhucarp.decisionprocess.collaborative.events;

import gphhucarp.core.Arc;
import gphhucarp.core.Graph;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessEvent;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.collaborative.CollaborativeDecisionProcess;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.reactive.event.ReactiveRefillEvent;
import gphhucarp.decisionprocess.reactive.event.ReactiveServingEvent;
import gphhucarp.decisionprocess.reactive.event.StaticEvent;
import gphhucarp.decisionprocess.routingpolicy.VehicleEvaluator_RoutingPolicy;
import gphhucarp.representation.route.NodeSeqRoute;

/**
 * The reactive refill event occurs when the vehicle is going back to the depot.
 * The target node is the depot, and there is no next task.
 * The vehicle does not have the next task to serve,
 * as no remaining task is feasible before the refill.
 * When it arrives the depot and finish the refill, it will start serving again.
 */

public class CollaborativeRefillEvent extends CollaborativeEvent {
    private NodeSeqRoute route;

    public CollaborativeRefillEvent(double time,
									NodeSeqRoute route) {
        super(time);
        this.route = route;
    }

    @Override
    public void trigger(DecisionProcess decisionProcess) {
        RoutingPolicy policy = decisionProcess.getRoutingPolicy();
        DecisionProcessState state = decisionProcess.getState();
        Instance instance = state.getInstance();
        Graph graph = instance.getGraph();
        int depot = instance.getDepot();

        int currNode = route.currNode();

        if (currNode == depot) {
            route.setDemand(0);

            if (state.getUnassignedTasks().isEmpty()) {
                // if there is no unassigned tasks, then no need to go out again.
                // stay at the depot and close the route
                route.setStatic();
                return;
            }

            // calculate the route-to-task map
            state.calcRouteToTaskMap(route);

//            if(route.getNodeSequence().size() > 1) {
//                if (policy instanceof VehicleEvaluator_RoutingPolicy)
//                    ((VehicleEvaluator_RoutingPolicy) policy).updateMatrix(state.getUnassignedTasks(), decisionProcess);
//                //                ((VehicleEvaluator_RoutingPolicy) policy).clearThenUpdateMatrix(route, route.getQueuedTasks(), decisionProcess);
//            }

            ReactiveDecisionSituation rds = new ReactiveDecisionSituation(state.getUnassignedTasks(), route, state);
            Arc nextTask = policy.next(rds, decisionProcess);
            route.setNextTask(nextTask);

            if(nextTask == null) {
                route.setNextTask(instance.getDepotLoop());
                route.setStatic();
                decisionProcess.getEventQueue().add(
                        new StaticEvent(route.getCost(), route));
            } else if(nextTask.getFrom() == 1 && nextTask.getTo() == 1) {
                decisionProcess.getEventQueue().add(
                        new CollaborativeRefillEvent(route.getCost(), route));
            } else {
                state.removeUnassignedTasks(nextTask);
                decisionProcess.getEventQueue().add(
                        new CollaborativeServingEvent(route.getCost(), route, nextTask));
            }
        }
        else {
            // continue going to the depot if not arrived yet
            int nextNode = graph.getPathTo(currNode, depot);

            // check the accessibility of all the arcs going out from the arrived node.
            // recalculate the shortest path based on the new cost matrix.
            // update the next node in the new shortest path.
            while (checkEdgeFailure(graph, currNode, instance, nextNode)) {
                graph.recalcEstDistanceBetween(currNode, depot);
                nextNode = graph.getPathTo(currNode, depot);
            }

            double remainingCapacity = route.getCapacity() - route.getDemand();

            if(CollaborativeDecisionProcess.refillType == 1 && remainingCapacity > 0) {
                altRefill(decisionProcess, currNode, nextNode, remainingCapacity);
            } else {
                // add the traversal to the next node as normal
                route.add(nextNode, 0, instance);
            }

            // add a new event
            decisionProcess.getEventQueue().add(
                    new CollaborativeRefillEvent(route.getCost(), route));

//            if(policy instanceof VehicleEvaluator_RoutingPolicy)
//                ((VehicleEvaluator_RoutingPolicy) policy).updateMatrix(state.getUnassignedTasks(), decisionProcess);
//                ((VehicleEvaluator_RoutingPolicy) policy).clearThenUpdateMatrix(route, route.getQueuedTasks(), decisionProcess);
        }
    }

    /**
     * Only for use if utilising the alternate refill type: checking if you can partially serve each task on
     * the route to the depot.
     *
     * @param currNode
     * @param nextNode
     * @param remainingCapacity
     */
    private void altRefill(DecisionProcess dp, int currNode, int nextNode, double remainingCapacity){
        DecisionProcessState state = dp.getState();
        Instance instance = state.getInstance();
        Graph graph = instance.getGraph();

        Arc task = graph.getArc(currNode, nextNode);

        if(task.isTask() && state.getTaskRemainingDemandFrac(task) > 0){
            double remainingDemandFraction = state.getTaskRemainingDemandFrac(task);
            double remainingDemand = instance.getActDemand(task) * remainingDemandFraction;

            if(remainingCapacity >= remainingDemand) {
                // ie: we can serve the whole thing.

                CollaborativeServingEvent cseTemp;
                CollaborativeServingEvent cse = null;
                if(!state.getUnassignedTasks().contains(task)) {
                    // identify which route is currently assigned to this task (if any)
                    for(DecisionProcessEvent event: dp.getEventQueue()) {
                        if (event.getClass().equals(CollaborativeServingEvent.class)) {
                            cseTemp = (CollaborativeServingEvent) event;
                            if (cseTemp.route.getNextTask() == task || cseTemp.route.getNextTask() == task.getInverse()) {
                                cse = cseTemp;
                                break;
                            }
                        }
                    }
                }

                // finish whatever is left
                double servedFraction = state.getTaskRemainingDemandFrac(task);
//                if(servedFraction != 1.0) System.out.println("ServedFraction: " + servedFraction);
                route.add(nextNode, servedFraction, instance);

                // remove the task from the remaining tasks
                state.removeRemainingTasks(task);
                state.removeUnassignedTasks(task);

                // update the task-to-task and route-to-task maps
                state.completeTask(task);

                // calculate the route-to-task map
                state.calcRouteToTaskMap(route);

                if(cse != null)
                    // i.e. steal the task from another vehicle
                    cse.assignNewTask(dp);
            } else {
                // we can't serve the whole thing (route failure equivalent)
                double servedFraction = remainingCapacity / instance.getActDemand(task);

                route.add(nextNode, servedFraction, instance);
                state.calcRouteToTaskMap(route);
            }
        } else {
            route.add(nextNode, 0, instance);
        }

//        if(dp.getRoutingPolicy() instanceof VehicleEvaluator_RoutingPolicy)
//            ((VehicleEvaluator_RoutingPolicy) dp.getRoutingPolicy()).updateMatrix(state.getUnassignedTasks(), dp);
////          ((VehicleEvaluator_RoutingPolicy) dp.getRoutingPolicy()).clearThenUpdateMatrix(route, route.getQueuedTasks(), state);
//
         }
}
