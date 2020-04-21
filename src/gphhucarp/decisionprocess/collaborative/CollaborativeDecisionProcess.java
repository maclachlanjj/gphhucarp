package gphhucarp.decisionprocess.collaborative;

import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.DecisionProcessEvent;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.poolfilter.collaborative.CollaborativeFilter;
import gphhucarp.decisionprocess.collaborative.events.CollaborativeRefillEvent;
import gphhucarp.representation.Solution;
import gphhucarp.representation.route.NodeSeqRoute;
import gphhucarp.representation.route.TaskSeqRoute;

import java.util.PriorityQueue;

public class CollaborativeDecisionProcess extends DecisionProcess{
	// defines which of the refill event types should be used. Set in EvaluationModel
	public static int refillType;
		// if 0, standard method of fastest route to the depot upon route failure (the default)
		// if 1, on way to depot, expend any excess capacity on tasks en route

	public static CollaborativeFilter pf;

	public CollaborativeDecisionProcess(DecisionProcessState state,
								   PriorityQueue<DecisionProcessEvent> eventQueue,
								   RoutingPolicy routingPolicy) {
		super(state, eventQueue, routingPolicy, null);
	}

	/**
	 * Initialise a collaborative decision process from an instance and a routing policy.
	 * @param instance the given instance.
	 * @param routingPolicy the given policy.
	 * @return the initial reactive decision process.
	 */
	public static CollaborativeDecisionProcess initCollaborative(Instance instance, long seed,
													   RoutingPolicy routingPolicy) {
		DecisionProcessState state = new DecisionProcessState(instance, seed);
		PriorityQueue<DecisionProcessEvent> eventQueue = new PriorityQueue<>();
		for (NodeSeqRoute route : state.getSolution().getRoutes())
			eventQueue.add(new CollaborativeRefillEvent(0, route));

		return new CollaborativeDecisionProcess(state, eventQueue, routingPolicy);
	}

	@Override
	public void reset() {
		state.reset();
		eventQueue.clear();
		for (NodeSeqRoute route : state.getSolution().getRoutes()) {
			eventQueue.add(new CollaborativeRefillEvent(0, route));
		}
	}

	@Override
	public DecisionProcess getInstance(DecisionProcessState state_clone, PriorityQueue<DecisionProcessEvent> eventQueue_clone,
									   RoutingPolicy policy_clone, Solution<TaskSeqRoute> plan_clone) {
		return new CollaborativeDecisionProcess(state_clone, eventQueue_clone, policy_clone);
	}

	@Override
	protected CollaborativeDecisionProcess clone() {
		DecisionProcessState clonedState = state.clone();
		PriorityQueue<DecisionProcessEvent> clonedEQ = new PriorityQueue<>(eventQueue);

		return new CollaborativeDecisionProcess(clonedState, clonedEQ, routingPolicy);
	}
}
