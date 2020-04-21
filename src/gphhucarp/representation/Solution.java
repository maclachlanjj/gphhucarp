package gphhucarp.representation;

import gphhucarp.core.Arc;
import gphhucarp.core.Instance;
import gphhucarp.core.InstanceSamples;
import gphhucarp.core.Objective;
import gphhucarp.representation.route.NodeSeqRoute;
import gphhucarp.representation.route.Route;
import gphhucarp.representation.route.TaskSeqRoute;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * A solution is represented as a list of routes.
 * The routes can be represented in different ways.
 * Therefore, the solution is a generic type with respect to the route parameter.
 *
 * Created by gphhucarp on 25/08/17.
 */

public class Solution<T extends Route> {
    private List<T> routes;

    public Solution(List<T> routes) {
        this.routes = routes;
    }

    /**
     * Construct empty routes.
     */
    public Solution() {
        this(new ArrayList<>());
    }

    public List<T> getRoutes() {
        return routes;
    }

    public void setRoutes(List<T> routes) {
        this.routes = routes;
    }

    public T getRoute(int index) {
        return routes.get(index);
    }

    /**
     * Add a route into the solution.
     * @param route the added route.
     */
    public void addRoute(T route) {
        routes.add(route);
    }

    /**
     * Remove the route with an index.
     * @param index the index of the route to be removed.
     */
    public void removeRoute(int index) {
        routes.remove(index);
    }

    /**
     * Initialise a task sequence solution of an instance.
     * Each route is an initial task sequence route.
     * @param instance the instance.
     * @return the initial task sequence solution.
     */
    public static Solution<TaskSeqRoute> initialTaskSeqSolution(Instance instance) {
        Solution<TaskSeqRoute> solution = new Solution<>();
        for (int i = 0; i < instance.getNumVehicles(); i++)
            solution.addRoute(TaskSeqRoute.initial(instance));

        return solution;
    }

    /**
     * Initialise a node sequence solution of an instance.
     * Each route is an initial node sequence route.
     * @param instance the instance.
     * @param numRoutes the number of routes.
     * @return the initial node sequence solution.
     */
    public static Solution<NodeSeqRoute> initialNodeSeqSolution(Instance instance,
                                                                int numRoutes) {
        Solution<NodeSeqRoute> solution = new Solution<>();
        for (int i = 0; i < numRoutes; i++)
            solution.addRoute(NodeSeqRoute.initial(instance));

        return solution;
    }

    /**
     * Initialise a node sequence solution of an instance.
     * Each route is an initial node sequence route.
     * @param instance the instance.
     * @return the initial node sequence solution.
     */
    public static Solution<NodeSeqRoute> initialNodeSeqSolution(Instance instance) {
        return initialNodeSeqSolution(instance, instance.getNumVehicles());
    }

    /**
     * Reset this solution under an instance. This is done by reseting each route.
     * @param instance the given instance.
     */
    public void reset(Instance instance) {
        for (int i = 0; i < instance.getNumVehicles(); i++)
            routes.get(i).reset(instance);
    }

    /**
     * Calculate the total cost, which is the sum of the route costs.
     * @return the total cost.
     */
    public double totalCost() {
        double result = 0;
        for (T route : routes)
            result += route.getCost();

        return result;
    }

    /**
     * Calculate the maximal route cost, i.e. makespan.
     * @return the maximal route cost.
     */
    public double maxRouteCost() {
        double result = -1;
        for (T route : routes) {
            if (result < route.getCost())
                result = route.getCost();
        }

        return result;
    }

    public double varianceCost(){
        ArrayList<Double> temp = new ArrayList<>();

        for (T route : routes)
            temp.add(route.getCost());

        Double[] data = temp.toArray(new Double[temp.size()]);

        double mean = 0.0;
        for (int i = 0; i < data.length; i++) {
            mean += data[i];
        }
        mean /= data.length;

        // The variance
        double variance = 0;
        for (int i = 0; i < data.length; i++)
            variance += (data[i] - mean) * (data[i] - mean);

        variance /= data.length;

        return variance;
    }

    /**
     * Return the value of an objective, NaN if the objective cannot calculated.
     * @param objective the objective.
     * @return the objective value of the solution.
     */
    public double objValue(Objective objective) {
        switch (objective) {
            case TOTAL_COST:
                return totalCost();
            case MAX_ROUTE_COST:
                return maxRouteCost();
            case VARIANCE:
                return varianceCost();
            default:
                return Double.NaN;
        }
    }

    public boolean isComplete(InstanceSamples sample){
        List<Arc> tasks = sample.getBaseInstance().getTasks();
        double[][] servedRecord = new double[tasks.size()][tasks.size()];

        for(Route r_temp: routes){
            NodeSeqRoute r = (NodeSeqRoute) r_temp;
            List<Integer> nodeSeq = r.getNodeSequence();
            List<Double> fracSeq = r.getFracSequence();
            for(int i = 0; i < nodeSeq.size() - 1; i++){
                int from = nodeSeq.get(i);
                int to = nodeSeq.get(i + 1);
                double frac = fracSeq.get(i);

                servedRecord[from][to] += frac;
            }
        }

        DecimalFormat df = new DecimalFormat("#.##########");
        for(Arc task: tasks){
            int from = task.getFrom(); int to = task.getTo();
            double sum = servedRecord[from][to] + servedRecord[to][from];
            sum = Double.valueOf(df.format(sum));
            // caught a scenario where the solver solved 0.9999999999999999 of a task, through two failures and one final serve,
            // which lead to an infeasible solution. Instead, round the sum at the tenth decimal.
            if(Double.compare(sum, 1) != 0) return false;
        }

        return true;
    }

    @Override
    public String toString() {
        String str = "Solution: \n";
        for (int i = 0; i < routes.size(); i++) {
            str = str + "route " + i + ": ";
            str = str + routes.get(i).toString() + " \n";
        }

        return str;
    }

    public Solution<T> clone() {
        Solution<T> clonedSol = new Solution<>();
        for (T route : routes)
            clonedSol.addRoute((T)route.clone());

        return clonedSol;
    }
}
