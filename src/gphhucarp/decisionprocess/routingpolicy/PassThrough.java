package gphhucarp.decisionprocess.routingpolicy;

import ec.EvolutionState;
import ec.gp.GPTree;
import ec.util.Parameter;
import gphhucarp.decisionprocess.PoolFilter;

public class PassThrough extends GPRoutingPolicy_frame {

    public PassThrough(){}

    public PassThrough(PoolFilter poolFilter, GPTree[] gpTrees) {
        super(poolFilter, gpTrees);
    }

    @Override
    public void setup(EvolutionState state, Parameter base) {

    }

    @Override
    public GPRoutingPolicy_frame newTree(PoolFilter poolFilter, GPTree[] gpTrees) {
        return new PassThrough(poolFilter, gpTrees);
    }

    @Override
    public boolean recordingData() {
        return false;
    }
}
