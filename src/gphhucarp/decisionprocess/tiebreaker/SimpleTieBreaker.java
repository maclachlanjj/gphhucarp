package gphhucarp.decisionprocess.tiebreaker;

import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.TieBreaker;

import java.util.List;

/**
 * A simple tie breaker between two arcs uses the natural comparator.
 *
 * Modified by JJM on 1/5/2020 to accommodate chains. Functionality otherwise the same.
 */

public class SimpleTieBreaker extends TieBreaker {
    @Override
    public int breakTie(List<Arc> chain1, List<Arc> chain2) { return chain1.get(0).compareTo(chain2.get(0)); }
}
