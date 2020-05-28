package gphhucarp.decisionprocess.tiebreaker;

import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.TieBreaker;
import org.apache.commons.math3.random.RandomDataGenerator;

import java.util.List;

public class RandomTieBreaker extends TieBreaker {

    private RandomDataGenerator rdg;

    public RandomTieBreaker(RandomDataGenerator rdg) {
        this.rdg = rdg;
    }

    @Override
    public int breakTie(List<Arc> chain1, List<Arc> chain2) {
        Arc arc1 = chain1.get(0); Arc arc2 = chain2.get(0);
        double r = rdg.nextUniform(0, 1);

        if (r < 0.5)
            return -1;

        return 1;
    }
}
