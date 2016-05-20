package annotatorstub.annotator.smaph;

import annotatorstub.utils.PythonApiInterface;
import annotatorstub.utils.mention.SmaphCandidate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class SmaphSRemoteSvmPruner implements SmaphSListPruner {

    private PythonApiInterface svmApi;

    public SmaphSRemoteSvmPruner(PythonApiInterface svmApi) {
        this.svmApi = svmApi;
    }

    @Override
    public List<SmaphCandidate> shouldKeep(List<SmaphCandidate> candidates) {
        try {
            List<Double> scores = new ArrayList<>();
            for(SmaphCandidate candidate : candidates) {
                scores.add(svmApi.binClassifyFlaskProbabilistic(candidate.getFeatures()));
            }

            // TODO(Taivo) this is where the magic should happen:
            // pick highest-scored non-overlapping candidates until a threshold is reached
            
            return candidates;
        }
        catch(IOException ex) {
            throw new RuntimeException("Could not access Python API.", ex);
        }
    }
}
