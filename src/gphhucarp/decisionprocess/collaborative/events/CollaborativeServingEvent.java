package gphhucarp.decisionprocess.collaborative.events;

import gphhucarp.core.Arc;
import gphhucarp.core.Graph;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.VehicleEvaluator_RoutingPolicy;
import gphhucarp.representation.route.NodeSeqRoute;

import java.util.List;

/**
 * The reactive serving event occurs when the vehicle is on the way
 * to serve the next task. Its target node is the head node of the next task.
 * The event is triggered whenever the vehicle arrives a node along the path.
 */

public class CollaborativeServingEvent extends CollaborativeEvent {
    public NodeSeqRoute route;

    public CollaborativeServingEvent(double time,
									 NodeSeqRoute route, Arc nextTask) {
        super(time);
        this.route = route;

        route.setNextTask(nextTask);
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

        Arc nextTask = route.getNextTask();

        if (currNode == nextTask.getFrom()) {
            // start serving the next task if it arrives its head node
            double remainingCapacity = instance.getCapacity() - route.getDemand();
            double remainingDemand = instance.getActDemand(nextTask) * state.getTaskRemainingDemandFrac(nextTask);

            if (remainingDemand > remainingCapacity) {
                // a route failure occurs; consume what you can and refill
                double servedFraction = remainingCapacity / instance.getActDemand(nextTask);

                // add the partial service to the route
                route.add(nextTask.getTo(), servedFraction, instance);

                // update the remaining demand fraction of the task

//                boolean colab = state.getTaskRemainingDemandFrac(nextTask) < 1;

                state.setTaskRemainingDemandFrac(nextTask, state.getTaskRemainingDemandFrac(nextTask) - servedFraction);
                state.setTaskRemainingDemandFrac(nextTask.getInverse(), state.getTaskRemainingDemandFrac(nextTask.getInverse()) - servedFraction);

//                if(colab) System.out.println("inv post frac: " + state.getTaskRemainingDemandFrac(nextTask.getInverse()));

                // add a new event: go to the depot to refill

                decisionProcess.getEventQueue().add(
                        new CollaborativeRefillEvent(route.getCost(), route));

                // add the partially complete task back to the unassigned task set so other vehicles can grab it.
                state.getUnassignedTasks().add(nextTask);
                state.getUnassignedTasks().add(nextTask.getInverse());

                route.setNextTask(instance.getDepotLoop()); // got to update the practical position of this node

                // reset this vehicle's queue when a route failure occurs

//                if(policy instanceof VehicleEvaluator_RoutingPolicy)
//                    ((VehicleEvaluator_RoutingPolicy) policy).updateMatrix(state.getUnassignedTasks(), decisionProcess);

//                    ((VehicleEvaluator_RoutingPolicy) policy).clearThenUpdateMatrix(route, route.getQueuedTasks(), decisionProcess);
                // don't need to set nextTask to anything as the refillEvent doesn't require one, so it's just ignored.

                 }
            else {
                // no route failure occurs, complete the service successfully (either in full, or part)
                route.add(nextTask.getTo(), state.getTaskRemainingDemandFrac(nextTask), instance);

                state.setTaskRemainingDemandFrac(nextTask, 0);
                decisionProcess.getEventQueue().add(
                        new CollaborativeServingEvent(route.getCost(), route, nextTask));
                // ensure extra decision step between 'completing' a task, and deciding on the next
            }
        }
        else if(currNode == nextTask.getTo()
                && Double.compare(state.getTaskRemainingDemandFrac(nextTask),0) == 0) {
            standardTaskCompleteAndAssign(decisionProcess);
        }
        else {
            // go to the next node if has not arrived the target node yet
            int nextNode = graph.getPathTo(currNode, nextTask.getFrom());

            // check the accessibility of all the arcs going out from the arrived node.
            // recalculate the shortest path based on the new cost matrix.
            // update the next node in the new shortest path.
            while (checkEdgeFailure(graph, currNode, instance, nextNode)) {
                // update knowledge about state of the graph
                graph.recalcEstDistanceBetween(currNode, nextTask.getFrom());
                nextNode = graph.getPathTo(currNode, nextTask.getFrom());
            }

            // traverse to the next node
            route.add(nextNode, 0, instance);
            // add a new event
            decisionProcess.getEventQueue().add(
                    new CollaborativeServingEvent(route.getCost(), route, nextTask));

//            if(policy instanceof VehicleEvaluator_RoutingPolicy)
////                ((VehicleEvaluator_RoutingPolicy) policy).updateMatrix(state.getUnassignedTasks(), decisionProcess);
//                ((VehicleEvaluator_RoutingPolicy) policy).clearThenUpdateMatrix(route, route.getQueuedTasks(), decisionProcess);
        }
    }

    public void assignNewTask(DecisionProcess decisionProcess){
        RoutingPolicy policy = decisionProcess.getRoutingPolicy();
        DecisionProcessState state = decisionProcess.getState();

        ReactiveDecisionSituation rds = new ReactiveDecisionSituation(state.getUnassignedTasks(), route, state);
        route.setNextTask(policy.next(rds, decisionProcess));

        decisionProcess.getEventQueue().remove(this);

        nextTaskCheck(decisionProcess, route.getNextTask(), route);
    }

    /**
     * Simply complete the current task and assign a new one.
     *
     * @param decisionProcess
     */
    private void standardTaskCompleteAndAssign(DecisionProcess decisionProcess){
        RoutingPolicy policy = decisionProcess.getRoutingPolicy();
        DecisionProcessState state = decisionProcess.getState();

        state.removeRemainingTasks(route.getNextTask());
        state.removeUnassignedTasks(route.getNextTask());

        // update the task-to-task and route-to-task maps
        state.completeTask(route.getNextTask());

        // calculate the route-to-task map
        state.calcRouteToTaskMap(route);

        // reset this vehicle's queue when it successfully completes a task

//        if(policy instanceof VehicleEvaluator_RoutingPolicy)
//            ((VehicleEvaluator_RoutingPolicy) policy).updateMatrix(state.getUnassignedTasks(), decisionProcess);
////                ((VehicleEvaluator_RoutingPolicy) policy).clearThenUpdateMatrix(route, route.getQueuedTasks(), decisionProcess);


        ReactiveDecisionSituation rds = new ReactiveDecisionSituation(state.getUnassignedTasks(), route, state);
        route.setNextTask(policy.next(rds, decisionProcess));

        nextTaskCheck(decisionProcess, route.getNextTask(), route);
    }
}

