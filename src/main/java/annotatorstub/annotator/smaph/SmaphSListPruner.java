package annotatorstub.annotator.smaph;

import annotatorstub.utils.mention.SmaphCandidate;

import java.util.List;

public interface SmaphSListPruner {
    List<SmaphCandidate> shouldKeep(List<SmaphCandidate> candidates);
}
