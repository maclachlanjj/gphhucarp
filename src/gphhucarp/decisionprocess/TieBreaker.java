package gphhucarp.decisionprocess;

import gphhucarp.core.Arc;

import java.util.List;

/**
 * A tie breaker breaks the tie between two arcs when they have the same priority.
 */

public abstract class TieBreaker {

    public abstract int breakTie(List<Arc> chain1, List<Arc> chain2);
}
