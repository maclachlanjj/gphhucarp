package gphhucarp.decisionprocess.collaborative.events;

import gphhucarp.core.Arc;
import gphhucarp.core.Graph;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.representation.route.NodeSeqRoute;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The collaborative refill-then-serve event occurs after a route failure occurs.
 * The vehicle is on the way back to the depot to refill, and then coming back
 * to finish the failed service.
 * The target node is the depot, and the next task is the failed task.
 *
 * Note: this is only constructed in the event of a route failure: i.e. when the
 * vehicle has no remaining capacity.
 */

public class CollaborativeRefillThenServeEvent extends CollaborativeEvent {

    private NodeSeqRoute route;

    public CollaborativeRefillThenServeEvent(double time,
                                             NodeSeqRoute route, Arc nextTask) {
        super(time);
        this.route = route;
        route.setNextTaskChain(Stream.of(nextTask).collect(Collectors.toList()));
    }

    @Override
    public void trigger(DecisionProcess decisionProcess) {
        DecisionProcessState state = decisionProcess.getState();
        Instance instance = state.getInstance();
        Graph graph = instance.getGraph();
        int depot = instance.getDepot();

        int currNode = route.currNode();

        if (currNode == depot) {
            // refill when arriving the depot
            route.setDemand(0);

            // now going back to continue the failed service
            // this is essentially a reactive serving event
            decisionProcess.getEventQueue().add(
                    new CollaborativeServingEvent(route.getCost(), route, route.getNextTask()));
        }
        else {
            // continue going to the depot if not arrived yet
            int nextNode = graph.getPathTo(currNode, depot);

            // check the accessibility of all the arcs going out from the arrived node.
            // recalculate the shortest path based on the new cost matrix.
            // update the next node in the new shortest path.
            while (checkEdgeFailure(graph, currNode, instance, nextNode)) {
                // update knowledge about state of the graph
                graph.recalcEstDistanceBetween(currNode, route.getNextTask().getFrom());
                nextNode = graph.getPathTo(currNode, route.getNextTask().getFrom());
            }

            // add the traverse to the next node
            route.add(nextNode, 0, instance);
            // add a new event
            decisionProcess.getEventQueue().add(
                    new CollaborativeRefillThenServeEvent(route.getCost(), route, route.getNextTask()));
        }
    }
}
