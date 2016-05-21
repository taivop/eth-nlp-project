package annotatorstub.annotator.smaph;

import annotatorstub.utils.mention.SmaphCandidate;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a simple pruner which handles every candidate in the candidate list individually by
 * delegating to a {@link Smaph1Pruner}.
 */
public class SmaphSIndividualPruner implements SmaphSListPruner {

    private Smaph1Pruner individualPruner;

    public SmaphSIndividualPruner(Smaph1Pruner individualPruner) {
        this.individualPruner = individualPruner;
    }

    @Override
    public List<SmaphCandidate> shouldKeep(List<SmaphCandidate> candidates) {
        return candidates.stream().filter(individualPruner::shouldKeep)
            .collect(Collectors.toList());
    }
}
