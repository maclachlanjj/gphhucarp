package gphhucarp.decisionprocess.routingpolicy.switches;

import gphhucarp.core.Arc;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.representation.route.NodeSeqRoute;

import java.util.HashSet;
import java.util.List;

/**
 * If the other vehicle's can feasibly solve the remaining demand, leave them to do so. Otherwise,
 * leave the depot and continue to serve the remaining tasks.
 */
public class CanStopSwitch {
    public static boolean canStop(ReactiveDecisionSituation rds, DecisionProcess dp, HashSet<NodeSeqRoute> decidedToStop){
        DecisionProcessState state = dp.getState();
        NodeSeqRoute route = rds.getRoute();
        Instance instance = state.getInstance();

        double sumRemainingDemand = 0;
        for(Arc arc: state.getRemainingTasks()){
            double frac = state.getTaskRemainingDemandFrac(arc);
            if (Double.compare(frac, 1.0) < 0) sumRemainingDemand += frac * instance.getActDemand(arc);
            else sumRemainingDemand += arc.getExpectedDemand();
        }
        double remainingDemand = sumRemainingDemand / 2; // as each arc is counted in remaining tasks twice (each direction)

        int numActiveVehicles = 0;

        double maxCapacity = instance.getCapacity();
        double remainingCapacity = 0;
//        int numTrips = 0;
        for(NodeSeqRoute altRoute: state.getSolution().getRoutes()) {
//            numTrips += altRoute.getNumTrips(instance);
            boolean isActive = altRoute != route && altRoute.currNode() != instance.getDepot() && !decidedToStop.contains(altRoute);
            if(isActive) {
                numActiveVehicles++;
                remainingCapacity += maxCapacity - altRoute.getDemand(); // .getDemand() only considers the route's current *trip*, not the entire route
            }
        }

        int numRoutes = state.getSolution().getRoutes().size();

        double leftArg = ((remainingDemand - remainingCapacity) / maxCapacity) + numActiveVehicles;
        double rightArg = numRoutes - decidedToStop.size() - 1;

        return leftArg <= rightArg;
    }
}
