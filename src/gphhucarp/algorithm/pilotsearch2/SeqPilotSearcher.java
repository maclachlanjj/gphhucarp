package gphhucarp.algorithm.pilotsearch2;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import ec.EvolutionState;
import ec.gp.GPTree;
import ec.util.Parameter;
import gphhucarp.algorithm.pilotsearch.PilotSearcher;
import gphhucarp.algorithm.pilotsearch2.event.PilotSearchRefillEvent;
import gphhucarp.algorithm.pilotsearch2.event.PilotSearchServingEvent;
import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.*;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionProcess;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.reactive.event.ReactiveRefillEvent;
import gphhucarp.decisionprocess.reactive.event.ReactiveServingEvent;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy_frame;
import gphhucarp.representation.route.NodeSeqRoute;

import java.util.PriorityQueue;

/**
 * The sequential pilot searcher starts from the current decision state,
 * and grow the routes one by one, until all the routes have been closed.
 */

public class SeqPilotSearcher extends GPRoutingPolicy_frame {

    public SeqPilotSearcher(){}

    public SeqPilotSearcher(PoolFilter poolFilter, GPTree[] gpTrees){
        super(poolFilter, gpTrees);
    }

    public Arc next(ReactiveDecisionSituation rds, DecisionProcess dp) {
        double startTime = System.currentTimeMillis();
        GPRoutingPolicy_frame original_policy = (GPRoutingPolicy_frame) dp.getRoutingPolicy(); // this is a SeqPilotSearcher policy <-- we don't want an infinite loop of these
        RoutingPolicy policy = new GPRoutingPolicy(original_policy.getPoolFilter(), original_policy.getGPTrees());
        DecisionProcessState state = rds.getState().clone();

//        System.out.println("pool size: " + rds.getPool().size());

        int decisionRouteId = rds.getState().getSolution().getRoutes().indexOf(rds.getRoute());

        int routeId = 0; // start from the first route
        PriorityQueue<DecisionProcessEvent> queue = new PriorityQueue<>();
        // build the routes before the decision route first
        while (routeId < decisionRouteId) {
            NodeSeqRoute route = state.getSolution().getRoute(routeId);
            Arc routeNextTask = route.getNextTask();

            if (routeNextTask.equals(state.getInstance().getDepotLoop())) {
                queue.add(new PilotSearchRefillEvent(route.getCost(), route));
            } else {
                queue.add(new PilotSearchServingEvent(route.getCost(), route, routeNextTask));
            }

            ReactiveDecisionProcess rdp = new ReactiveDecisionProcess(state, queue, policy);

            while (!queue.isEmpty()) {
                DecisionProcessEvent event = queue.poll();
                event.trigger(rdp);

                if (route.currNode() == state.getInstance().getDepot())
                    queue.clear();
            }

            routeId ++; // the final routeId == decisionRouteId
        }

        // get the next task of the current route
        NodeSeqRoute route = state.getSolution().getRoute(routeId);
        Arc routeNextTask = route.getNextTask();
        DecisionProcessEvent event;
        if (routeNextTask.equals(state.getInstance().getDepotLoop())) {
            event = new PilotSearchRefillEvent(route.getCost(), route);
        } else {
            event = new PilotSearchServingEvent(route.getCost(), route, routeNextTask);
        }

        ReactiveDecisionProcess rdp = new ReactiveDecisionProcess(state, queue, policy);
        // trigger the current event to get a new next task
        event.trigger(rdp);

//        System.out.println("route " + routeId + " serving from " + route.getNextTask().getFrom() + " to " + route.getNextTask().getTo());

        int depot = state.getInstance().getDepot();

        double endTime = System.currentTimeMillis();
        sumDecisionTime += endTime - startTime; numDecisions++;

        if(route.getNextTask().getFrom() == depot && route.getNextTask().getTo() == depot) return null;
        return route.getNextTask();
    }

    @Override
    public void setup(EvolutionState state, Parameter base) {

    }

    @Override
    public GPRoutingPolicy_frame newTree(PoolFilter poolFilter, GPTree[] gpTrees) {
        return new SeqPilotSearcher(poolFilter, gpTrees);
    }

    @Override
    public boolean recordingData() {
        return false;
    }
}
