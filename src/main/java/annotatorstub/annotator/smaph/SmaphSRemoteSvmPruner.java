package annotatorstub.annotator.smaph;

import annotatorstub.utils.PythonApiInterface;
import annotatorstub.utils.mention.SmaphCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class SmaphSRemoteSvmPruner implements SmaphSListPruner {
    private static Logger logger = LoggerFactory.getLogger(SmaphSRemoteSvmPruner.class);

    private PythonApiInterface svmApi;
    public static final Double PRUNING_THRESHOLD = 0.65;

    public SmaphSRemoteSvmPruner(PythonApiInterface svmApi) {
        this.svmApi = svmApi;
    }

    @Override
    public List<SmaphCandidate> shouldKeep(List<SmaphCandidate> candidates) {
        try {
            PriorityQueue<SmaphCandidate> pq = new PriorityQueue<>();

            for (SmaphCandidate candidate : candidates) {
                double score = svmApi.binClassifyFlaskProbabilistic(candidate.getFeatures());
                candidate.setScore(score);
                pq.add(candidate);
            }

            if (! pq.isEmpty()) {
                logger.info("Best candidate score: " + pq.peek().getScore());
            }

            List<SmaphCandidate> acceptedCandidates = new ArrayList<>();
            while (! pq.isEmpty()) {
                SmaphCandidate scoredCandidate = pq.poll();

                // If the current candidate is below threshold, we know all the subsequent ones will be, too.
                if (scoredCandidate.getScore() < PRUNING_THRESHOLD) {
                    break;
                }

                // Accept only candidates that don't overlap with previously chosen ones.
                if (! scoredCandidate.overlapsAny(acceptedCandidates)) {
                    acceptedCandidates.add(scoredCandidate);
                }
            }

            return acceptedCandidates;
        }
        catch (IOException ex) {
            throw new RuntimeException("Could not access Python API.", ex);
        }
    }
}
