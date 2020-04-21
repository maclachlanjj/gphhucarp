package gputils.FileOutput;

import ec.gp.GPTree;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.representation.Solution;
import gphhucarp.representation.route.NodeSeqRoute;

import java.util.HashSet;

public class DualTree_FileOutput extends FileOutput {
    private static HashSet<String> makespanRecord;

    public static void setupDataCollection(GPTree indTree){
        if(makespanRecord == null)
            makespanRecord = new HashSet<>();
    }

    public static String saveSolution(DecisionProcessState state, Solution<NodeSeqRoute> sol, double fitness){
        Instance inst = state.getInstance();
        String res = inst.getTotalInstanceDemand() + ", " + fitness;

        for(NodeSeqRoute route: sol.getRoutes()){
            double demand = route.getRouteDemandServed(state);
            int trips = route.getNumTrips(inst);
            res += ", " + demand + ", " + trips;
        }

        makespanRecord.add(res);
        return res;
    }
}
