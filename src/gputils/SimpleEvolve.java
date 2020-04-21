package gputils;

import ec.EvolutionState;
import ec.Evolve;
import ec.util.Output;
import ec.util.Parameter;
import ec.util.ParameterDatabase;
import gputils.FileOutput.FileOutput;
import gputils.FileOutput.Rollout_FileOutput;

/**
 * A simple evolution process, extended/simplified from ec.Evolve.
 * It can be a GA optmisation or GPHH training process.
 *
 * It will produce a file xxx.out.stat.
 *
 * Created by YiMei on 12/09/16.
 */

public class SimpleEvolve extends Evolve {

    public static void main(String[] args) {
        EvolutionState state;
        ParameterDatabase parameters;

        parameters = loadParameterDatabase(args);
        int numJobs = parameters.getIntWithDefault(new Parameter("jobs"), null, 1);
        if (numJobs < 1)
            Output.initialError("The 'jobs' parameter must be >= 1 (or not exist, which defaults to 1)");

        // Now we know how many jobs remain.  Let's loop for that many jobs.  Each time we'll
        // load the parameter database scratch (except the first time where we reuse the one we
        // just loaded a second ago).  The reason we reload from scratch each time is that the
        // experimenter is free to scribble all over the parameter database and it'd be nice to
        // have everything fresh and clean.  It doesn't take long to load the database anyway,
        // it's usually small.
        for(int job = 0; job < numJobs; job++)
        {
            // We used to have a try/catch here to catch errors thrown by this job and continue to the next.
            // But the most common error is an OutOfMemoryException, and printing its stack trace would
            // just create another OutOfMemoryException!  Which dies anyway and has a worthless stack
            // trace as a result.

            // try
            {
                // load the parameter database (reusing the very first if it exists)
                if (parameters == null)
                    parameters = loadParameterDatabase(args);

                // Initialize the EvolutionState, then set its job variables
                state = initialize(parameters, job);                // pass in job# as the seed increment
                state.output.systemMessage("Job: " + job);
                state.job = new Object[1];                                  // make the job argument storage
                state.job[0] = job;                    // stick the current job in our job storage
                state.runtimeArguments = args;                              // stick the runtime arguments in our storage

                if (numJobs > 1)                                                    // only if iterating (so we can be backwards-compatible),
                {
                    String jobFilePrefix = "job." + job + ".";
                    state.output.setFilePrefix(jobFilePrefix);     // add a prefix for checkpoint/output files
                    state.checkpointPrefix = jobFilePrefix + state.checkpointPrefix;  // also set up checkpoint prefix
                }

                // Here you can set up the EvolutionState's parameters further before it's setup(...).
                // This includes replacing the random number generators, changing values in state.parameters,
                // changing instance variables (except for job and runtimeArguments, please), etc.

                // here we're having to read the seed.0 param from scratch because it's not stored in the Evolve file.
                // use the value to make the X different output files to record the number of times each route actively
                // returns to the depot.
                Parameter p = new Parameter(P_SEED).push(""+0);
                int seed = state.parameters.getInt(p, null, 0);
                FileOutput.setParams(seed, "outputData_");

                // now we let it go
                state.run(EvolutionState.C_STARTED_FRESH);

                cleanup(state);  // flush and close various streams, print out parameters if necessary
                parameters = null;  // so we load a fresh database next time around
            }
            Rollout_FileOutput.exit();

            /*
              catch (Throwable e)  // such as an out of memory error caused by this job
              {
              e.printStackTrace();
              state = null;
              System.gc();  // take a shot!
              }
            */
        }

//        System.exit(0);
    }
}
