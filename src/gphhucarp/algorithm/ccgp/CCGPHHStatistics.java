package gphhucarp.algorithm.ccgp;

import ec.EvolutionState;
import ec.Individual;
import ec.simple.SimpleProblemForm;
import ec.simple.SimpleStatistics;

/**
 * The CCGPHH statistics.
 * It prints out the context vector for each generation.
 */

public class CCGPHHStatistics extends SimpleStatistics {

    @Override
    public void postEvaluationStatistics(final EvolutionState state) {

        CCGPHHEvolutionState ccgpState = (CCGPHHEvolutionState)state;

        // print the best-of-generation individual
        if (doGeneration)
            state.output.println("\nGeneration: " + state.generation,statisticslog);

        if (doGeneration)
            state.output.println("Best Individual:",statisticslog);

        for(int x = 0; x < state.population.subpops.length; x++) {
            if (doGeneration)
                state.output.println("Subpopulation " + x + ":",statisticslog);

            if (doGeneration)
                ccgpState.getContext(x).printIndividualForHumans(state,statisticslog);

            if (doMessage && !silentPrint) state.output.message("Subpop " + x + " best fitness of generation" +
                    (ccgpState.getContext(x).evaluated ? " " : " (evaluated flag not set): ") +
                    ccgpState.getContext(x).fitness.fitnessToStringForHumans());

            // describe the winner if there is a description
            if (doGeneration && doPerGenerationDescription) {
                if (state.evaluator.p_problem instanceof SimpleProblemForm)
                    ((SimpleProblemForm)(state.evaluator.p_problem.clone())).describe(state, ccgpState.getContext(x), x, 0, statisticslog);
            }
        }
    }
}
