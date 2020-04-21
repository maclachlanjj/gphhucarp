package gphhucarp.decisionprocess.collaborative.events;

import gphhucarp.core.Arc;
import gphhucarp.core.Graph;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.*;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.VehicleEvaluator_RoutingPolicy;
import gphhucarp.representation.route.NodeSeqRoute;

import java.util.List;

public abstract class CollaborativeEvent extends DecisionProcessEvent {
	public CollaborativeEvent(double time){ super(time); }

	public abstract void trigger(DecisionProcess decisionProcess);

	public boolean checkEdgeFailure(Graph graph, int currNode, Instance instance, int nextNode){
		boolean isEdgeFailure = false;

		List<Arc> list = graph.getOutNeighbour(currNode);

		for (Arc arc : list) {
			if (instance.getActDeadheadingCost(arc) == Double.POSITIVE_INFINITY) {
				graph.updateEstCostMatrix(arc.getFrom(), arc.getTo(), Double.POSITIVE_INFINITY);

				if (arc.getTo() == nextNode)
					isEdgeFailure = true;
			}
		}

		return isEdgeFailure;
	}

	public void refillAndAssignNextTask(NodeSeqRoute route, DecisionProcessState state, RoutingPolicy policy, DecisionProcess decisionProcess){
		// refill when arriving the depot
		route.setDemand(0);

		if (state.getUnassignedTasks().isEmpty()) {
			// if there is no unassigned tasks, then no need to go out again.
			// stay at the depot and close the route
			return;
		}
		// calculate the route-to-task map
		state.calcRouteToTaskMap(route);

//		if(policy instanceof VehicleEvaluator_RoutingPolicy)
//			((VehicleEvaluator_RoutingPolicy) policy).updateMatrix(state.getUnassignedTasks(), state);
////			((VehicleEvaluator_RoutingPolicy) policy).clearThenUpdateMatrix(route, route.getQueuedTasks(), state);

		ReactiveDecisionSituation rds = new ReactiveDecisionSituation(state.getUnassignedTasks(), route, state);
		Arc nextTask = policy.next(rds, decisionProcess);

		nextTaskCheck(decisionProcess,nextTask,route);
	}


	public void nextTaskCheck(DecisionProcess decisionProcess, Arc nextTask, NodeSeqRoute route){
		DecisionProcessState state = decisionProcess.getState();

		if (nextTask == null || (nextTask.getFrom() == 1 && nextTask.getTo() == 1)) {
			// go back to refill if there is no feasible task to serve next
			route.setNextTask(state.getInstance().getDepotLoop());
			decisionProcess.getEventQueue().add(
					new CollaborativeRefillEvent(route.getCost(), route));
		}
		else {
			// assigning the next task
			route.setNextTask(nextTask);
			state.removeUnassignedTasks(nextTask);
			decisionProcess.getEventQueue().add(
					new CollaborativeServingEvent(route.getCost(), route, nextTask));
		}
	}
}
