package annotatorstub.utils.mention;


import it.unipi.di.acube.batframework.data.Mention;

import java.util.Collection;

/**
 * Simple wrapper class to attach scores to SmaphCandidates.
 */
public class SmaphCandidateScored implements Comparable<SmaphCandidateScored> {

    private SmaphCandidate smaphCandidate;
    private Double score;

    public SmaphCandidateScored(SmaphCandidate smaphCandidate, Double score) {
        this.smaphCandidate = smaphCandidate;
        this.score = score;
    }

    public SmaphCandidate getSmaphCandidate() {
        return smaphCandidate;
    }

    public Double getScore() {
        return score;
    }

    public int compareTo(SmaphCandidateScored that) {
//        return Double.compare(this.score, that.score);
        return Double.compare(that.score, this.score);
    }

    public boolean overlaps(SmaphCandidateScored that) {
        MentionCandidate thisMentionCandidate = this.smaphCandidate.getMentionCandidate();
        MentionCandidate thatMentionCandidate = that.smaphCandidate.getMentionCandidate();

        Mention thisMention =
                new Mention(thisMentionCandidate.getQueryStartPosition(),thisMentionCandidate.getLength());
        Mention thatMention =
                new Mention(thatMentionCandidate.getQueryStartPosition(), thatMentionCandidate.getLength());

        return thisMention.overlaps(thatMention);
    }

    public boolean overlapsAny(Collection<SmaphCandidateScored> collection) {
        for(SmaphCandidateScored that : collection) {
            if(this.overlaps(that))
                return true;
        }
        return false;
    }


}
