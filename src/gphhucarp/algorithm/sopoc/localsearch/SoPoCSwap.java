package gphhucarp.algorithm.sopoc.localsearch;

import ec.Individual;
import gphhucarp.algorithm.edasls.EdgeHistogramMatrix;
import gphhucarp.algorithm.edasls.GiantTaskSequenceIndividual;
import gphhucarp.algorithm.sopoc.SoPoCEvolutionState;
import gphhucarp.algorithm.sopoc.SoPoCProblem;
import gphhucarp.core.Arc;

import java.util.LinkedList;
import java.util.List;

/**
 * The swap operator defines a move as swapping two tasks.
 * It pads the sequence by depot loops to address boundary issues.
 *
 */
public class SoPoCSwap extends SoPoCLocalSearch {

    @Override
    public GiantTaskSequenceIndividual move(SoPoCEvolutionState state, GiantTaskSequenceIndividual curr) {
        EdgeHistogramMatrix ehm = state.getEhm();
        SoPoCProblem problem = (SoPoCProblem)state.evaluator.p_problem;

        Individual[] inds = new Individual[state.population.subpops.length];
        boolean[] updates = new boolean[state.population.subpops.length];

        // initialise inds as the context vector
        for(int i = 0; i < state.population.subpops.length; i++) {
            inds[i] = state.getContext(i);
            updates[i] = false;
        }

        // evaluate subpop 0: the baseline solution
        updates[0] = true;

        List<Arc> seq = new LinkedList<>(curr.getTaskSequence());

        // pad the sequence by depot loop
        seq.add(state.getUcarpInstance().getDepotLoop());
        seq.add(0, state.getUcarpInstance().getDepotLoop());

        GiantTaskSequenceIndividual neighbour;

        for (int origPos = 1; origPos < seq.size()-3; origPos++) {
            Arc task1 = seq.get(origPos);
            Arc invTask1 = task1.getInverse();

            Arc origPre = seq.get(origPos-1); // original predecessor
            Arc origSuc = seq.get(origPos+1); // original successor

            // avoid swapping adjacent tasks, which is essentially the same as single insertion
            for (int targPos = origPos+2; targPos < seq.size()-1; targPos++) {
                Arc task2 = seq.get(targPos);
                Arc invTask2 = task2.getInverse();

                Arc targPre = seq.get(targPos-1); // target predecessor
                Arc targSuc = seq.get(targPos+1); // target successor

                double oldEhm = ehm.getValue(origPre, task1) +
                        ehm.getValue(task1, origSuc) +
                        ehm.getValue(targPre, task2) +
                        ehm.getValue(task2, targSuc);

                // pre-check the delta ehm of swapping task1 and task2
                double newEhm = ehm.getValue(origPre, task2) +
                        ehm.getValue(task2, origSuc) +
                        ehm.getValue(targPre, task1) +
                        ehm.getValue(task1, targSuc);

                if (newEhm <= oldEhm)
                    continue;

                // the new ehm is greater than the old ehm, apply the move
                neighbour = curr.clone();
                neighbour.getTaskSequence().set(origPos-1, task2);
                neighbour.getTaskSequence().set(targPos-1, task1);

                inds[0] = neighbour;
                problem.evaluate(state, inds, updates, false, new int[state.population.subpops.length], 0);
                state.EDASLSFEs[state.generation] ++;

                if (neighbour.fitness.betterThan(curr.fitness))
                    return neighbour;

                // pre-check the delta ehm of swaping (task1, invTask2)
                newEhm = ehm.getValue(origPre, invTask2) +
                        ehm.getValue(invTask2, origSuc) +
                        ehm.getValue(targPre, task1) +
                        ehm.getValue(task1, targSuc);

                if (newEhm <= oldEhm)
                    continue;

                // the new ehm is greater than the old ehm, apply the move
                neighbour = curr.clone();
                neighbour.getTaskSequence().set(origPos-1, invTask2);
                neighbour.getTaskSequence().set(targPos-1, task1);

                inds[0] = neighbour;
                problem.evaluate(state, inds, updates, false, new int[state.population.subpops.length], 0);
                state.EDASLSFEs[state.generation] ++;

                if (neighbour.fitness.betterThan(curr.fitness))
                    return neighbour;

                // pre-check the delta ehm of swaping (invTask1, task2)
                newEhm = ehm.getValue(origPre, task2) +
                        ehm.getValue(task2, origSuc) +
                        ehm.getValue(targPre, invTask1) +
                        ehm.getValue(invTask1, targSuc);

                if (newEhm <= oldEhm)
                    continue;

                // the new ehm is greater than the old ehm, apply the move
                neighbour = curr.clone();
                neighbour.getTaskSequence().set(origPos-1, task2);
                neighbour.getTaskSequence().set(targPos-1, invTask1);

                inds[0] = neighbour;
                problem.evaluate(state, inds, updates, false, new int[state.population.subpops.length], 0);
                state.EDASLSFEs[state.generation] ++;

                if (neighbour.fitness.betterThan(curr.fitness))
                    return neighbour;

                // pre-check the delta ehm of swaping (invTask1, invTask2)
                newEhm = ehm.getValue(origPre, invTask2) +
                        ehm.getValue(invTask2, origSuc) +
                        ehm.getValue(targPre, invTask1) +
                        ehm.getValue(invTask1, targSuc);

                if (newEhm <= oldEhm)
                    continue;

                // the new ehm is greater than the old ehm, apply the move
                neighbour = curr.clone();
                neighbour.getTaskSequence().set(origPos-1, invTask2);
                neighbour.getTaskSequence().set(targPos-1, invTask1);

                inds[0] = neighbour;
                problem.evaluate(state, inds, updates, false, new int[state.population.subpops.length], 0);
                state.EDASLSFEs[state.generation] ++;

                if (neighbour.fitness.betterThan(curr.fitness))
                    return neighbour;
            }
        }

        return curr;
    }
}
