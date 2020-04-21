package gphhucarp.decisionprocess.reactive.event;

import gphhucarp.decisionprocess.DecisionProcessEvent;
import gphhucarp.representation.route.NodeSeqRoute;

public abstract class ReactiveEvent extends DecisionProcessEvent {
    protected NodeSeqRoute route;

    public ReactiveEvent(double time, NodeSeqRoute r) {
        super(time);
        this.route = r;
    }

    public NodeSeqRoute getRoute(){
        return route;
    }

    public abstract DecisionProcessEvent deepClone(NodeSeqRoute route);
}
