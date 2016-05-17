package annotatorstub.annotator.smaph;

import annotatorstub.utils.mention.SmaphCandidate;

public interface Smaph1Pruner {
    boolean shouldKeep(SmaphCandidate candidate);
}
