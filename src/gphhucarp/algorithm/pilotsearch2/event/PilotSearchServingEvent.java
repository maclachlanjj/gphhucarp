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
import gphhucarp.decisionprocess.reactive.event.ReactiveServingEvent;
import gphhucarp.representation.route.NodeSeqRoute;

import java.util.LinkedList;
import java.util.List;

/**
 * This event is almost the same as ReactiveServingEvent.java.
 * The only difference is that when selecting the next task,
 * it does a pilot search from the current state.
 * It uses the expected information to do the pilot search.
 */

public class PilotSearchServingEvent extends ReactiveEvent {
    private Arc nextTask;

    public PilotSearchServingEvent(double time, NodeSeqRoute route,
                                   Arc nextTask) {
        super(time, route);
        this.nextTask = nextTask;
    }

    @Override
    public void trigger(DecisionProcess decisionProcess) {
        RoutingPolicy policy = decisionProcess.getRoutingPolicy();
        DecisionProcessState state = decisionProcess.getState();
        Instance instance = state.getInstance();
        Graph graph = instance.getGraph();
        int depot = instance.getDepot();

        int currNode = route.currNode();

        // refill the capacity if the current node is the depot
        if (currNode == depot)
            route.setDemand(0);

        if (currNode == nextTask.getFrom()) {
            // start serving the next task if it arrives its head node
            double remainingCapacity = instance.getCapacity() - route.getDemand();
            double nextTaskRemainingFrac = state.getTaskRemainingDemandFrac(nextTask);
//            double remainingDemand = instance.getActDemand(nextTask) * nextTaskRemainingFrac;
            double remainingDemand = nextTask.getExpectedDemand() * nextTaskRemainingFrac;

            if (remainingDemand > remainingCapacity) {
                // a route failure occurs, refill and then come back
                double servedFraction = remainingCapacity / remainingDemand;

                // add the partial service to the route
                route.addPilot(nextTask.getTo(), servedFraction, instance);
                // update the remaining demand fraction of the task
                state.setTaskRemainingDemandFrac(nextTask, nextTaskRemainingFrac - servedFraction);
                // add a new event: go to the depot to refill, and come back to
                // continue the failed service.
                decisionProcess.getEventQueue().add(
                        new PilotSearchRefillThenServeEvent(route.getCost(), route, nextTask));
            }
            else {
                // no route failure occurs, can complete the service successfully
                route.addPilot(nextTask.getTo(), nextTaskRemainingFrac, instance);
                // update the remaining demand fraction of the task
                state.setTaskRemainingDemandFrac(nextTask, 0.0);
                // remove the task from the remaining tasks
                state.removeRemainingTasks(nextTask);
                // update the task-to-task and route-to-task maps
                state.completeTask(nextTask);
                // add a new serving event.
                decisionProcess.getEventQueue().add(
                        new PilotSearchServingEvent(route.getCost(), route, nextTask));
            }
        }
        else if (currNode == nextTask.getTo() &&
                Double.compare(state.getTaskRemainingDemandFrac(nextTask), 0.0) == 0) {
            // calculate the route-to-task map
            state.calcRouteToTaskMap(route);

            List<Arc> pool = new LinkedList<>(state.getUnassignedTasks());

            // decide which task to serve next by pilot search
            ReactiveDecisionSituation rds = new ReactiveDecisionSituation(
                    pool, route, state);

            nextTask = policy.next(rds, decisionProcess);

            if (nextTask == null || nextTask.equals(instance.getDepotLoop())) {
                // go back to the depot to refill, if the depot loop is selected
                route.setNextTask(instance.getDepotLoop());

                decisionProcess.getEventQueue().add(
                        new PilotSearchRefillEvent(route.getCost(), route));
            }
            else {
                state.removeUnassignedTasks(nextTask);
                route.setNextTask(nextTask);
                decisionProcess.getEventQueue().add(
                        new PilotSearchServingEvent(route.getCost(), route, nextTask));
            }
        }
        else {
            // go to the next node if has not arrived the target node yet
            int nextNode = graph.getPathTo(currNode, nextTask.getFrom());

            // in the expectation there is no edge failure

            // add the traverse to the next node
            route.addPilot(nextNode, 0, instance);
            // add a new event
            decisionProcess.getEventQueue().add(
                    new PilotSearchServingEvent(route.getCost(), route, nextTask));
        }
    }

    @Override
    public DecisionProcessEvent deepClone(NodeSeqRoute route) {
        return new PilotSearchServingEvent(this.time, route, this.nextTask);
    }
}
