package gphhucarp.decisionprocess.routingpolicy.ensemble.combiner;

import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.routingpolicy.ensemble.EnsemblePolicy;
import gphhucarp.decisionprocess.routingpolicy.ensemble.Combiner;
import gphhucarp.representation.route.NodeSeqRoute;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The aggregator combiner simply sums up the weighted priority calculated by all the elements,
 * and set the final priority as the weighted sum.
 */

public class Aggregator extends Combiner {

    @Override
    public List<Arc> next(List<Arc> pool, NodeSeqRoute route, DecisionProcessState state, EnsemblePolicy ensemblePolicy) {
        HashMap<List<Arc>, Double> priorities = new HashMap<>();

        List<Arc> next = Stream.of(pool.get(0)).collect(Collectors.toList());
        priorities.put(next, priority(next, route, state, ensemblePolicy));

        for (int i = 1; i < pool.size(); i++) {
            List<Arc> tmp = Stream.of(pool.get(i)).collect(Collectors.toList());
            priorities.put(tmp, priority(tmp, route, state, ensemblePolicy));

            if (Double.compare(priorities.get(tmp), priorities.get(next)) < 0 ||
                    (Double.compare(priorities.get(tmp), priorities.get(next)) == 0 &&
                            ensemblePolicy.getTieBreaker().breakTie(tmp, next) < 0))
                next = tmp;
        }

        return next;
    }

    /**
     * Calculate the priority of a candidate arc by an ensemble policy.
     * @param arc the arc whose priority is to be calculated.
     * @param route the route.
     * @param state the decision process state.
     * @param ensemblePolicy the ensemble policy.
     * @return the priority of the arc calculated by the ensemble policy.
     */
    private double priority(List<Arc> arc, NodeSeqRoute route, DecisionProcessState state, EnsemblePolicy ensemblePolicy) {
        double priority = 0;
        for (int i = 0; i < ensemblePolicy.size(); i++) {
            priority += ensemblePolicy.getPolicy(i).priority(arc, route, state) *
                    ensemblePolicy.getWeight(i);
        }

        return priority;
    }
}
