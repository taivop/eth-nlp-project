package annotatorstub.annotator.smaph;

import annotatorstub.utils.PythonApiInterface;
import annotatorstub.utils.mention.SmaphCandidate;

import java.io.IOException;

public class Smaph1RemoteSvmPruner implements Smaph1Pruner {

    private PythonApiInterface svmApi;

    public Smaph1RemoteSvmPruner(PythonApiInterface svmApi) {
        this.svmApi = svmApi;
    }

    @Override
    public boolean shouldKeep(SmaphCandidate candidate) {
        try {
            return svmApi.binClassifyFlask(candidate.getFeatures());
        }
        catch(IOException ex) {
            throw new RuntimeException("Could not access Python API.", ex);
        }
    }
}
