package work.ready.examples.distributed_computing;

import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskSplitAdapter;

import java.util.ArrayList;
import java.util.List;

public class LetterCountComputeTask extends ComputeTaskSplitAdapter<String, Integer> {

    @Override
    public List<ComputeJob> split(int gridSize, String arg) {
        // 1. Splits the received string into words
        String[] words = arg.split(" ");

        List<ComputeJob> jobs = new ArrayList<>(words.length);

        // 2. Creates a child job for each word
        for (final String word : words) {
            jobs.add(new ComputeJobAdapter() {
                @Override
                public Object execute() {
                    System.err.println(">>> Printing '" + word + "' from compute job.");

                    // Return the number of letters in the word.
                    return word.length();
                }
            });
        }

        // 3. Sends the jobs to other nodes for processing.
        return jobs;
    }

    @Override
    public Integer reduce(List<ComputeJobResult> results) {
        int sum = 0;

        for (ComputeJobResult res : results) {
            sum += res.<Integer>getData();
        }
        return sum;
    }
}
