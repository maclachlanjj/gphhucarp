package gphhucarp.decisionprocess;

import ec.gp.GPTree;
import gphhucarp.algorithm.pilotsearch.PilotSearcher;
import gphhucarp.algorithm.pilotsearch.event.PilotSearchRefillEvent;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.proreactive.ProreativeDecisionProcess;
import gphhucarp.decisionprocess.proreactive.event.ProreactiveServingEvent;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionProcess;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.reactive.event.ReactiveEvent;
import gphhucarp.decisionprocess.reactive.event.ReactiveRefillEvent;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy_frame;
import gphhucarp.decisionprocess.routingpolicy.VehicleEvaluator_RoutingPolicy;
import gphhucarp.representation.Solution;
import gphhucarp.representation.route.NodeSeqRoute;
import gphhucarp.representation.route.TaskSeqRoute;

import java.util.PriorityQueue;

/**
 * An abstract of a decision process. A decision process is a process where
 * vehicles make decisions as they go to serve the tasks of the graph.
 * It includes
 *  - A decision process state: the state of the vehicles and the environment
 *  - An event queue: the events to happen
 *  - A routing policy that makes decisions as the vehicles go.
 *  - A task sequence solution as a predefined plan. This is used for proactive-reactive decision process.
 */

public abstract class DecisionProcess {
    protected DecisionProcessState state; // the state
    protected PriorityQueue<DecisionProcessEvent> eventQueue;
    protected RoutingPolicy routingPolicy;
    protected Solution<TaskSeqRoute> plan;

    public DecisionProcess(DecisionProcessState state,
                           PriorityQueue<DecisionProcessEvent> eventQueue,
                           RoutingPolicy routingPolicy,
                           Solution<TaskSeqRoute> plan) {
        this.state = state;
        this.eventQueue = eventQueue;
        this.routingPolicy = routingPolicy;
        this.plan = plan;
    }

    public DecisionProcessState getState() {
        return state;
    }

    public PriorityQueue<DecisionProcessEvent> getEventQueue() {
        return eventQueue;
    }

    public RoutingPolicy getRoutingPolicy() {
        return routingPolicy;
    }

    public void setRoutingPolicy(RoutingPolicy routingPolicy) {
        this.routingPolicy = routingPolicy;
    }

    public Solution<TaskSeqRoute> getPlan() {
        return plan;
    }

    public void setPlan(Solution<TaskSeqRoute> plan) {
        this.plan = plan;
    }

    /**
     * Initialise a reactive decision process from an instance and a routing policy.
     * @param instance the given instance.
     * @param seed the seed to sample the random variables.
     * @param routingPolicy the given policy.
     * @return the initial reactive decision process.
     */
    public static ReactiveDecisionProcess initReactive(Instance instance,
                                                       long seed,
                                                       RoutingPolicy routingPolicy) {
        DecisionProcessState state = new DecisionProcessState(instance, seed);
        PriorityQueue<DecisionProcessEvent> eventQueue = new PriorityQueue<>();
        for (NodeSeqRoute route : state.getSolution().getRoutes())
            eventQueue.add(new ReactiveRefillEvent(0, route));

        return new ReactiveDecisionProcess(state, eventQueue, routingPolicy);
    }

    /**
     * Initialise a proactive-reactive decision process from an instance, a routing policy and a plan.
     * @param instance the given instance.
     * @param seed the seed.
     * @param routingPolicy the given policy.
     * @param plan the given plan (a task sequence solution).
     * @return the initial proactive-reactive decision process.
     */
    public static ProreativeDecisionProcess initProreactive(Instance instance,
                                                            long seed,
                                                            RoutingPolicy routingPolicy,
                                                            Solution<TaskSeqRoute> plan) {
        DecisionProcessState state = new DecisionProcessState(instance, seed, plan.getRoutes().size());
        PriorityQueue<DecisionProcessEvent> eventQueue = new PriorityQueue<>();
        for (int i = 0; i < plan.getRoutes().size(); i++)
            eventQueue.add(new ProreactiveServingEvent(0,
                    state.getSolution().getRoute(i), plan.getRoute(i), 0));

        return new ProreativeDecisionProcess(state, eventQueue, routingPolicy, plan);
    }

    /**
     * Initialise a pilot search decision process.
     * @param instance the given instance.
     * @param seed the seed.
     * @param routingPolicy the routing policy.
     * @param pilotSearcher the pilot searcher.
     * @return the initial pilot search decision process.
     */
    public static ReactiveDecisionProcess initPilotSearch(Instance instance,
                                                             long seed,
                                                             RoutingPolicy routingPolicy,
                                                             PilotSearcher pilotSearcher) {
        DecisionProcessState state = new DecisionProcessState(instance, seed);
        PriorityQueue<DecisionProcessEvent> eventQueue = new PriorityQueue<>();
        for (NodeSeqRoute route : state.getSolution().getRoutes())
            eventQueue.add(new PilotSearchRefillEvent(0, route, pilotSearcher));

        return new ReactiveDecisionProcess(state, eventQueue, routingPolicy);
    }

    /**
     * Run the decision process.
     */
    static int run = 0;
    public void run() {
        // first sample the random variables by the seed.
        state.getInstance().setSeed(state.getSeed());

//        System.out.println("----------------------- New run -----------------------");

//        double start = System.currentTimeMillis();

        // trigger the events.
        while (!eventQueue.isEmpty()) {
            DecisionProcessEvent event = eventQueue.poll();
            event.trigger(this);
        }

//        double end = System.currentTimeMillis();
//        double time = end - start;
//        System.out.println("time: " + time + " routing policy: " + routingPolicy.getClass());
    }

    public void runRollout(int steps){
        // don't want to resample the random variables (nor change all the run() dependent code)
        int currStep = 0;
        while (currStep < steps && !eventQueue.isEmpty()) {
            DecisionProcessEvent event = eventQueue.poll();
//            if(event instanceof ReactiveServingEvent)
                currStep++;
            event.trigger(this);
        }
    }

    /**
     * Reset the decision process.
     * This is done by reseting the decision process state and event queue.
     */
    public abstract void reset();

    /**
     * Produce numClones deep clones of the entire simulation state. Used frequently in rollout simulations.
     *
     * @param numClones
     * @return
     */
    public DecisionProcess[] getStateClones(ReactiveDecisionSituation rds, int numClones, int base){
        DecisionProcess[] res = new DecisionProcess[numClones];

        for(int i = 0; i < numClones; i++){
            int newSeedBase = (base == -1) ? -1 : i % base;
            DecisionProcessState state_clone = state.deepClone(rds, newSeedBase);
//            GPTree tree_clone = (GPTree)((GPRoutingPolicy_frame) routingPolicy).getGPTrees().clone(); // copy the current routing policy (the evolved rule)

            GPTree[] originalTrees = ((GPRoutingPolicy_frame) routingPolicy).getGPTrees();
            GPTree[] tree_clones = new GPTree[originalTrees.length];
            for(int k = 0; k < originalTrees.length; k++)
                tree_clones[k] = (GPTree) originalTrees[k].clone();

            RoutingPolicy policy_clone = new GPRoutingPolicy(routingPolicy.poolFilter, tree_clones); // DON'T use 'recursive' rollout, so defer to non-rollout routing logic
            Solution<TaskSeqRoute> plan_clone = null;
            if(plan != null) plan_clone = plan.clone(); // <--- contains TaskSeqRoutes (null check for when we start at the depot)

            // Original priority queue contains NodeSeqEvents, which have already been cloned within the State
            PriorityQueue<DecisionProcessEvent> eventQueue_clone = new PriorityQueue<>();
            for(DecisionProcessEvent event: eventQueue){
                ReactiveEvent reactiveEvent = (ReactiveEvent) event;
                NodeSeqRoute route_clone = state_clone.getRoute(state.getReturned(), reactiveEvent.getRoute());
                eventQueue_clone.add(reactiveEvent.deepClone(route_clone)); // just create a new instance using the previously cloned route
            }

            res[i] = this.getInstance(state_clone, eventQueue_clone, policy_clone, plan_clone);
        }

        return res;
    }

    public abstract DecisionProcess getInstance(DecisionProcessState state_clone, PriorityQueue<DecisionProcessEvent> eventQueue_clone,
                                                  RoutingPolicy policy_clone, Solution<TaskSeqRoute> plan_clone);

}
