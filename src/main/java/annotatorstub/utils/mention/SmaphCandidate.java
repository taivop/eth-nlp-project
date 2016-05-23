package annotatorstub.utils.mention;

import annotatorstub.annotator.smaph.SmaphSAnnotator;
import it.unipi.di.acube.batframework.data.Mention;

import java.util.Collection;
import java.util.List;

/**
 * A possible annotation, complete with numeric features which can be used for pruning with e.g.
 * an SVM classifier.
 */
public class SmaphCandidate implements Comparable<SmaphCandidate> {

    private final int entityID;
    private final MentionCandidate mentionCandidate;
    private final List<Double> features;

    /**
     * If applicable, holds a candidate's score (e.g. probability of relevance in the Smaph-S case).
     */
    private double score;

    public SmaphCandidate(
        int entityID,
        MentionCandidate mentionCandidate,
        List<Double> features
    ) {
    this(entityID, mentionCandidate, features, SmaphSAnnotator.DUMMY_SCORE);
    }

    public SmaphCandidate(
        int entityID,
        MentionCandidate mentionCandidate,
        List<Double> features,
        float score
    ) {
        this.entityID = entityID;
        this.mentionCandidate = mentionCandidate;
        this.features = features;
        this.score = score;
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

    public void setScore(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    @Override
    public int compareTo(SmaphCandidate that) {
        return Double.compare(that.score, this.score);
    }

    public boolean overlaps(SmaphCandidate that) {
        MentionCandidate thisMentionCandidate = this.getMentionCandidate();
        MentionCandidate thatMentionCandidate = that.getMentionCandidate();

        Mention thisMention =
            new Mention(thisMentionCandidate.getQueryStartPosition(),thisMentionCandidate.getLength());
        Mention thatMention =
            new Mention(thatMentionCandidate.getQueryStartPosition(), thatMentionCandidate.getLength());

        return thisMention.overlaps(thatMention);
    }

    public boolean overlapsAny(Collection<SmaphCandidate> collection) {
        for(SmaphCandidate that : collection) {
            if(this.overlaps(that)) {
                return true;
            }
        }

        return false;
    }



}
