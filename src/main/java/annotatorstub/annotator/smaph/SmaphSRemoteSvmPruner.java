package annotatorstub.annotator.smaph;

import annotatorstub.utils.PythonApiInterface;
import annotatorstub.utils.mention.SmaphCandidate;
import annotatorstub.utils.mention.SmaphCandidateScored;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public class SmaphSRemoteSvmPruner implements SmaphSListPruner {

    private PythonApiInterface svmApi;
    public static final Double PRUNING_THRESHOLD = 0.2;

    public SmaphSRemoteSvmPruner(PythonApiInterface svmApi) {
        this.svmApi = svmApi;
    }

    @Override
    public List<SmaphCandidate> shouldKeep(List<SmaphCandidate> candidates) {
        try {
            PriorityQueue<SmaphCandidateScored> pq = new PriorityQueue<>();

            for(SmaphCandidate candidate : candidates) {
                Double score = svmApi.binClassifyFlaskProbabilistic(candidate.getFeatures());
                pq.add(new SmaphCandidateScored(candidate, score));
            }

            List<SmaphCandidateScored> acceptedCandidates = new ArrayList<>();
            while(!pq.isEmpty()) {
                SmaphCandidateScored scoredCandidate = pq.poll();

                // If the current candidate is below threshold, we know all the subsequent ones will be, too.
                if(scoredCandidate.getScore() < PRUNING_THRESHOLD) {
                    break;
                }

                // Accept only candidates that don't overlap with previously chosen ones.
                if(!scoredCandidate.overlapsAny(acceptedCandidates)) {
                    acceptedCandidates.add(scoredCandidate);
                }
            }

            List<SmaphCandidate> acceptedCandidatesWithoutScores = acceptedCandidates.stream()
                    .map(SmaphCandidateScored::getSmaphCandidate)
                    .collect(Collectors.toList());

            return acceptedCandidatesWithoutScores;
        }
        catch(IOException ex) {
            throw new RuntimeException("Could not access Python API.", ex);
        }
    }
}
