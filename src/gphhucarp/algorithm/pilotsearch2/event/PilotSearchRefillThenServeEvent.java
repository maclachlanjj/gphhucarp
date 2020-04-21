package gphhucarp.algorithm.pilotsearch2.event;

import gphhucarp.algorithm.pilotsearch.PilotSearcher;
import gphhucarp.core.Arc;
import gphhucarp.core.Graph;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessEvent;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.reactive.event.ReactiveEvent;
import gphhucarp.representation.route.NodeSeqRoute;

/**
 * This is the same as ReactiveRefillThenServeEvent, but interact with other
 * pilot search events.
 */

public class PilotSearchRefillThenServeEvent extends ReactiveEvent {
    private Arc nextTask;

    public PilotSearchRefillThenServeEvent(double time, NodeSeqRoute route,
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

        if (currNode == depot) {
            // refill when arriving the depot
            route.setDemand(0);

            // now going back to continue the failed service
            // this is essentially a serving event
            decisionProcess.getEventQueue().add(
                    new PilotSearchServingEvent(route.getCost(), route, nextTask));
        }
        else {
            // continue going to the depot if not arrived yet
            int nextNode = graph.getPathTo(currNode, depot);

            // there is no edge failure in expectation

            // add the traverse to the next node
            route.addPilot(nextNode, 0, instance);
            // add a new event
            decisionProcess.getEventQueue().add(
                    new PilotSearchRefillThenServeEvent(route.getCost(), route, nextTask));
        }
    }

    @Override
    public DecisionProcessEvent deepClone(NodeSeqRoute route) {
        return null;
    }
}
