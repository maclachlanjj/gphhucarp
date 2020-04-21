package gphhucarp.decisionprocess.reactive.event;

import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessEvent;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.representation.route.NodeSeqRoute;

/**
 * Made simply to represent when a vehicle is staying at the depot
 */
public class StaticEvent extends ReactiveEvent {
    public StaticEvent(double time, NodeSeqRoute r) {
        super(time, r);
    }

    @Override
    public DecisionProcessEvent deepClone(NodeSeqRoute route) {
        return new StaticEvent(this.time, route);
    }

    @Override
    public void trigger(DecisionProcess decisionProcess) {
        DecisionProcessState state = decisionProcess.getState();
        Instance instance = state.getInstance();

        route.setNextTask(instance.getDepotLoop());
    }
}
