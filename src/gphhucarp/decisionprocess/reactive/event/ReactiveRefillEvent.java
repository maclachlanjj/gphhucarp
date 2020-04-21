package gphhucarp.decisionprocess.reactive.event;

import gphhucarp.core.Arc;
import gphhucarp.core.Graph;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.VehicleEvaluator_RoutingPolicy;
import gphhucarp.representation.route.NodeSeqRoute;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessEvent;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.RoutingPolicy;

import java.util.LinkedList;
import java.util.List;

/**
 * The reactive refill event occurs when the vehicle is going back to the depot.
 * The target node is the depot, and there is no next task.
 * The vehicle does not have the next task to serve,
 * as no remaining task is feasible before the refill.
 * When it arrives the depot and finish the refill, it will start serving again.
 */

public class ReactiveRefillEvent extends ReactiveEvent {

    public ReactiveRefillEvent(double time,
                               NodeSeqRoute route) {
        super(time, route);
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
            route.toggleActiveRefill(true);
            // refill when arriving the depot
            route.setDemand(0);

            if (state.getUnassignedTasks().isEmpty()) {
                // if there is no unassigned tasks, then no need to go out again.
                // stay at the depot and close the route
                return;
            }

            // calculate the route-to-task map
            state.calcRouteToTaskMap(route);

            if(policy instanceof VehicleEvaluator_RoutingPolicy)
//                ((VehicleEvaluator_RoutingPolicy) policy).updateMatrix(state.getUnassignedTasks(), state);
                ((VehicleEvaluator_RoutingPolicy) policy).clearThenUpdateMatrix(route, route.getQueuedTasks(), decisionProcess);

            // decide which task to serve next
            List<Arc> pool = new LinkedList<>(state.getUnassignedTasks());

            ReactiveDecisionSituation rds = new ReactiveDecisionSituation(
                    pool, route, state);

            Arc nextTask = policy.next(rds, decisionProcess);
            route.setNextTask(nextTask);

            if(nextTask == null) {
                route.setNextTask(instance.getDepotLoop());
                route.setStatic();
                decisionProcess.getEventQueue().add(
                        new StaticEvent(route.getCost(), route));
            } else if(nextTask.getFrom() == 1 && nextTask.getTo() == 1) {
                decisionProcess.getEventQueue().add(
                        new ReactiveRefillEvent(route.getCost(), route));
            } else {
                state.removeUnassignedTasks(nextTask);
                decisionProcess.getEventQueue().add(
                        new ReactiveServingEvent(route.getCost(), route, nextTask));
            }
        }
        else {
            // continue going to the depot if not arrived yet
            int nextNode = graph.getPathTo(currNode, depot);

            // check the accessibility of all the arcs going out from the arrived node.
            boolean edgeFailure = false; // edge failure: next node is not accessible.
            for (Arc arc : graph.getOutNeighbour(currNode)) {
                if (instance.getActDeadheadingCost(arc) == Double.POSITIVE_INFINITY) {
                    graph.updateEstCostMatrix(arc.getFrom(), arc.getTo(), Double.POSITIVE_INFINITY);

                    if (arc.getTo() == nextNode)
                        edgeFailure = true;
                }
            }

            // recalculate the shortest path based on the new cost matrix.
            // update the next node in the new shortest path.
            if (edgeFailure) {
                graph.recalcEstDistanceBetween(currNode, depot);
                nextNode = graph.getPathTo(currNode, depot);
            }

            // add the traverse to the next node
            route.add(nextNode, 0, instance);
            // add a new event
            decisionProcess.getEventQueue().add(
                    new ReactiveRefillEvent(route.getCost(), route));

            if(policy instanceof VehicleEvaluator_RoutingPolicy)
//                ((VehicleEvaluator_RoutingPolicy) policy).updateMatrix(state.getUnassignedTasks(), state);
                ((VehicleEvaluator_RoutingPolicy) policy).clearThenUpdateMatrix(route, route.getQueuedTasks(), decisionProcess);
        }
    }

    @Override
    public DecisionProcessEvent deepClone(NodeSeqRoute route) {
        return new ReactiveRefillEvent(this.time, route);
    }
}
