package gphhucarp.representation.route;

import gphhucarp.core.Arc;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.DecisionProcessState;

import java.util.*;

/**
 * A node sequence route is a sequence of nodes
 * plus a sequence indicating the fraction of demand served (0 if not served).
 * For example:
 * ----------------------------------
 * Node sequence:     [0,1,5,3,4,2,0]
 * Fraction sequence: [ 0,1,0,0,1,1 ]
 * ----------------------------------
 * means the route serves (1,5), (4,2) and (2,0).
 *
 * Note that the indicating vector can be double, i.e. a fraction of demand is served.
 *
 * Created by gphhucarp on 25/08/17.
 */
public class NodeSeqRoute extends Route {
    private List<Integer> nodeSequence;
    private List<Double> fracSequence;
    private boolean hasStopped;

    // whether or not the vehicle has decided to return to the depot intentionally,
    // over serving one of the remaining tasks (as opposed to because it is unable to)
    private boolean activeRefill = false;

    // fields used during the decision process
    private List<Arc> nextTaskChain; // the next task(s) to serve (depot loop if refilling). index 0 is next task

    public NodeSeqRoute(double capacity, double demand, double cost,
                        List<Integer> nodeSequence, List<Double> fracSequence) {
        super(capacity, demand, cost);
        this.nodeSequence = nodeSequence;
        this.fracSequence = fracSequence;
        this.hasStopped = false;
    }

    public NodeSeqRoute(double capacity) {
        this(capacity, 0, 0, new LinkedList<>(), new LinkedList<>());
    }

    public List<Integer> getNodeSequence() {
        return nodeSequence;
    }

    public List<Double> getFracSequence() {
        return fracSequence;
    }

    public int getNode(int index) {
        return nodeSequence.get(index);
    }

    public double getFraction(int index) { return fracSequence.get(index); }

    private int taskIndex;

    public void setNextTaskChain(List<Arc> chain) {
        this.nextTaskChain = chain;
        taskIndex = 0;
    }

    public void setNextTaskChain(List<Arc> chain, DecisionProcessState state) {
        if(chain != null)
            for(Arc a: chain)
                state.removeUnassignedTasks(a);
        this.nextTaskChain = chain;
        taskIndex = 0;
    }

    public Arc getNextTask() { if(nextTaskChain == null) return null; return nextTaskChain.get(taskIndex); }

    // called when the current task in the chain is completed.
    public void stepUpChain() { if(taskIndex < nextTaskChain.size()-1) taskIndex++; }

    public boolean hasNextTask() {
        Arc next = nextTaskChain.get(taskIndex);
        return (taskIndex < nextTaskChain.size()-1) && !(next.getFrom() == 1 && next.getTo() == 1);
    }

    public void setStatic(){
        this.hasStopped = true;
    }

    public void restart() { this.hasStopped = false; }

    public boolean hasStopped(){
        return hasStopped;
    }

    /**
     * Call this whenever the vehicle decides to actively return to the
     * depot, or it arrives at the depot.
     */
    private int numActiveRefills = 0;
    public void toggleActiveRefill(boolean arrivingAtDepot){
        if(arrivingAtDepot){
            activeRefill = false;
        } else {
            activeRefill = true;
            numActiveRefills++;
        }
    }

    public int getNumActiveRefills(){
        return numActiveRefills;
    }

    public int getNumTrips(Instance inst){
        int count = 0;

        boolean newTrip = true;
        for(int i = 1; i < nodeSequence.size(); i++){
            int curr = nodeSequence.get(i);
            double frac = fracSequence.get(i - 1);

            if(newTrip)
                if(Double.compare(frac, 0) > 0) {
                    count++;
                    newTrip = false;
                }

            if(curr == inst.getDepot()) newTrip = true;
        }

        return count;
    }

    public double getRouteDemandServed(DecisionProcessState state){
        double sumDemandServed = 0;
        Instance inst = state.getInstance();
        for(int i = 0; i < nodeSequence.size()-1; i++){
            int from = nodeSequence.get(i);
            int to = nodeSequence.get(i+1);
            // had to make a Arc[nodeA][nodeB] data structure in Instance to get this information
            Arc arc = state.getInstance().getArcBetweenNodes(from, to);


            double fracServed = fracSequence.get(i);
            double demandServed = inst.getActDemand(arc) * fracServed;
            sumDemandServed += demandServed;
        }
        return sumDemandServed;
    }

    /**
     * Add a node of an instance in a pilot search (not knowing the actual demand and cost)
     * @param node the node to be added.
     * @param fraction
     * @param instance
     */
    public void addPilot(int node, double fraction, Instance instance) {
        Arc arc = instance.getGraph().getArc(currNode(), node);

        nodeSequence.add(node);
        fracSequence.add(fraction);
        demand += arc.getExpectedDemand() * fraction;
        cost += arc.getServeCost() * fraction + arc.getExpectedDeadheadingCost() * (1-fraction);
    }

    /**
     * Add a node of an instance with a possible service.
     * @param node the node.
     * @param fraction the fraction of demand to be served (1 if fully served, 0 if not served).
     */
    public void add(int node, double fraction, Instance instance) {

        Arc arc = instance.getGraph().getArc(currNode(), node);

        nodeSequence.add(node);
        fracSequence.add(fraction);

        demand += instance.getActDemand(arc) * fraction;
        cost += arc.getServeCost() * fraction + instance.getActDeadheadingCost(arc) * (1-fraction);
    }

    /**
     * An initial node sequence route for an instance.
     * It starts from the depot.
     * @param instance the instance.
     * @return An initial node sequence route starting from the depot.
     */
    public static NodeSeqRoute initial(Instance instance) {
        NodeSeqRoute initialRoute = new NodeSeqRoute(instance.getCapacity());
        initialRoute.nodeSequence.add(instance.getDepot());

        return initialRoute;
    }

    @Override
    public void reset(Instance instance) {
        demand = 0;
        cost = 0;
        nodeSequence.clear();
        fracSequence.clear();
        nodeSequence.add(instance.getDepot());
    }

    @Override
    public int currNode() {
        return nodeSequence.get(nodeSequence.size()-1);
    }

    @Override
    public String toString() {
        String str = "" + nodeSequence.get(0);
        for (int i = 0; i < fracSequence.size(); i++) {
            if (fracSequence.get(i) == 0) {
                // simply traverse without serving
                str += " -> " + nodeSequence.get(i+1);
            }
            else {
                // serving the arc/task with the fraction of demand
                str += " (" + fracSequence.get(i) + ") " + nodeSequence.get(i+1);
            }
        }
        return str;
    }

    @Override
    public boolean equals(Object o1){
        NodeSeqRoute other = (NodeSeqRoute) o1;

        if(this.nextTaskChain.size() != other.nextTaskChain.size()) return false;

        for(int i = 0; i < nextTaskChain.size(); i++)
            if (this.nextTaskChain.get(i).compareTo(other.nextTaskChain.get(i)) != 0)
                return false;

//        if(this.nextTask.compareTo(other.nextTask) != 0)
//            return false;

        if(nodeSequence.size() != other.nodeSequence.size())
            return false;

        for(int i = 0; i < Math.min(nodeSequence.size(), other.nodeSequence.size()); i++){
            int thisNode = getNode(i); int otherNode = other.getNode(i);
            if(thisNode != otherNode)
                return false;
        }

        for(int i = 0; i < Math.min(fracSequence.size(), other.fracSequence.size()); i++){
            double thisFrac = getFraction(i); double otherFrac = other.getFraction(i);
            if(Double.compare(thisFrac, otherFrac) != 0)
                return false;
        }

        return true;
    }

    /**
     * Clone the node sequence route.
     * @return the cloned route.
     */
    @Override
    public Route clone() {
        List<Integer> clonedNodeSeq = new LinkedList<>(nodeSequence);
        List<Double> clonedFracSeq = new LinkedList<>(fracSequence);

        NodeSeqRoute cloned = new NodeSeqRoute(capacity, demand, cost, clonedNodeSeq, clonedFracSeq);

        cloned.nextTaskChain = new ArrayList<>(nextTaskChain);
        cloned.taskIndex = taskIndex;

        return cloned;
    }
}
