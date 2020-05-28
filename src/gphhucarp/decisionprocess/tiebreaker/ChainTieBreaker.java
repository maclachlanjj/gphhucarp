package gphhucarp.decisionprocess.tiebreaker;

import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.TieBreaker;

import java.util.List;

public class ChainTieBreaker extends TieBreaker {
    @Override
    public int breakTie(List<Arc> chain1, List<Arc> chain2) {
        Arc c1h = chain1.get(0);
        Arc c1t = chain1.get(chain1.size()-1);

        Arc c2h = chain2.get(0);
        Arc c2t = chain2.get(chain2.size()-1);

        if(c1h.getFrom() < c2h.getFrom())
            return -1;

        if(c1h.getFrom() > c2h.getFrom())
            return 1;

        if(c1t.getTo() < c2t.getTo())
            return -1;

        if(c1t.getTo() > c2t.getTo())
            return 1;

        return 0;
    }
}
