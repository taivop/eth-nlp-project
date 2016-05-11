package annotatorstub.utils.mention;

import java.util.List;

public class SmaphCandidate {

    private final int entityID;
    private final MentionCandidate mentionCandidate;
    private final List<Double> features;

    public SmaphCandidate(int entityID, MentionCandidate mentionCandidate, List<Double> features) {
        this.entityID = entityID;
        this.mentionCandidate = mentionCandidate;
        this.features = features;
    }

    public int getEntityID() {
        return entityID;
    }

    public MentionCandidate getMentionCandidate() {
        return mentionCandidate;
    }

    public List<Double> getFeatures() {
        return features;
    }
}
