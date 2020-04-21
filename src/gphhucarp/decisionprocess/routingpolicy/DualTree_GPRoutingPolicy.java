package gphhucarp.decisionprocess.routingpolicy;

import ec.gp.GPTree;
import gphhucarp.decisionprocess.PoolFilter;

public abstract class DualTree_GPRoutingPolicy extends GPRoutingPolicy_frame {
    public static boolean recordData;

    public DualTree_GPRoutingPolicy(){ super(); }

    public DualTree_GPRoutingPolicy(PoolFilter poolFilter, GPTree[] gpTree) {
        super(poolFilter, gpTree);
    }

    @Override
    public abstract GPRoutingPolicy_frame newTree(PoolFilter poolFilter, GPTree[] gpTrees);

    @Override
    public boolean recordingData() {
        return recordData;
    }
}
