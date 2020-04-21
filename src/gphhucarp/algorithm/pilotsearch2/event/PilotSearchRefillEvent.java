package gphhucarp.algorithm.pilotsearch2.event;

import gphhucarp.algorithm.pilotsearch.PilotSearcher;
import gphhucarp.core.Arc;
import gphhucarp.core.Graph;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessEvent;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.reactive.event.ReactiveEvent;
import gphhucarp.decisionprocess.reactive.event.ReactiveRefillEvent;
import gphhucarp.decisionprocess.reactive.event.ReactiveServingEvent;
import gphhucarp.decisionprocess.reactive.event.StaticEvent;
import gphhucarp.representation.route.NodeSeqRoute;

import java.util.LinkedList;
import java.util.List;

/**
 * This is the same as ReactiveRefillEvent, but interact with other pilot search events.
 */

public class PilotSearchRefillEvent extends ReactiveEvent {

    public PilotSearchRefillEvent(double time, NodeSeqRoute route) {
        super(time, route);
    }

    @Override
    public DecisionProcessEvent deepClone(NodeSeqRoute route) {
        return null;
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
            // refill when arriving the depot
            route.setDemand(0);

            if (state.getUnassignedTasks().isEmpty()) {
                // if there is no unassigned tasks, then no need to go out again.
                // stay at the depot and close the route
                return;
            }

            // calculate the route-to-task map
            state.calcRouteToTaskMap(route);

            List<Arc> pool = new LinkedList<>(state.getUnassignedTasks());

            // decide which task to serve next by pilot search
            ReactiveDecisionSituation rds = new ReactiveDecisionSituation(
                    pool, route, state);

            Arc nextTask = policy.next(rds, decisionProcess);
            route.setNextTask(nextTask);

            if(nextTask == null) {
                route.setNextTask(instance.getDepotLoop());
                route.setStatic();
                decisionProcess.getEventQueue().add(
                        new StaticEvent(route.getCost(), route));
            } else if(nextTask.getFrom() == 1 && nextTask.getTo() == 1)
                decisionProcess.getEventQueue().add(
                        new PilotSearchRefillEvent(route.getCost(), route));
            else {
                state.removeUnassignedTasks(nextTask);
                decisionProcess.getEventQueue().add(
                        new PilotSearchServingEvent(route.getCost(), route, nextTask));
            }
        }
        else {
            // continue going to the depot if not arrived yet
            int nextNode = graph.getPathTo(currNode, depot);

            // there is no edge failure in expectation

            // add the traverse to the next node
            route.addPilot(nextNode, 0, instance);
            // add a new event
            decisionProcess.getEventQueue().add(
                    new PilotSearchRefillEvent(route.getCost(), route));
        }
    }
}
