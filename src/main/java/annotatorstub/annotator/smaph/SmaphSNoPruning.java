package annotatorstub.annotator.smaph;

import annotatorstub.utils.mention.SmaphCandidate;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link SmaphSListPruner} which does NO pruning.
 */
public class SmaphSNoPruning implements SmaphSListPruner {
    @Override
    public List<SmaphCandidate> shouldKeep(List<SmaphCandidate> candidates) {
        return new ArrayList<>(candidates);
    }
}
