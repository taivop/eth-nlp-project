package annotatorstub.annotator.smaph;

import annotatorstub.annotator.FakeAnnotator;
import annotatorstub.annotator.tagme.TagMeAnnotator;
import annotatorstub.utils.WATRelatednessComputer;
import annotatorstub.utils.mention.GreedyMentionIterator;
import annotatorstub.utils.mention.MentionCandidate;
import com.sun.tools.javac.util.Pair;
import it.unipi.di.acube.batframework.data.ScoredAnnotation;
import it.unipi.di.acube.batframework.utils.AnnotationException;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * SMAPH-S annotator from the paper "A Piggyback System for Joint Entity Mention Detection and Linking in Web Queries".
 * @see http://www2016.net/proceedings/proceedings/p567.pdf
 */

public class SmaphSAnnotator extends FakeAnnotator {

    /**
     * Returns a set of candidate entities from the output of TagMeAnnotator.
     *
     * @param query the query as a string
     * @return a set of integers, each corresponding to a Wikipedia ID of a candidate entity.
     */
    public Set<Integer> getMockEpsilonSet1(String query) {
        HashSet<Integer> candidates = new HashSet<Integer>();

        TagMeAnnotator ann = new TagMeAnnotator();
        Set<ScoredAnnotation> scoredAnnotations = ann.solveSa2W(query);

        for(ScoredAnnotation scoredAnnotation : scoredAnnotations) {
            System.out.println(scoredAnnotation.getConcept());
            candidates.add(scoredAnnotation.getConcept());
        }

        return candidates;
    }

    public Set<Integer> getMockEpsilonSet2(String query) {
        return new HashSet<Integer>();
    }

    public Set<Integer> getMockEpsilonSet3(String query) {
        return new HashSet<Integer>();
    }

    /**
     * Returns the SMAPH-S features for given entity. See table 4 in article.
     *
     * @param entity the Wikipedia ID of the entity
     * @return vector of per-entity features
     */
    private Vector<Double> getEntityFeatures(Integer entity) {
        // TODO have a hashmap for this so if we query the same entity again, we won't recalculate stuff.

        return new Vector<>();
    }

    /**
     * Returns the SMAPH-S features for given mention-entity pair. See table 5 in article.
     *
     * @param mention the mention in query
     * @param entity the Wikipedia ID of the entity
     * @return vector of per-pair features
     */
    private Vector<Double> getMentionEntityFeatures(MentionCandidate mention, Integer entity) {
        // TODO
        return new Vector<>();
    }

    public HashSet<ScoredAnnotation> solveSa2W(String query) throws AnnotationException {

        // Create set of candidate entities
        Set<Integer> candidates = new HashSet<Integer>();               // Epsilon_q in the paper
        candidates.addAll(getMockEpsilonSet1(query));
        candidates.addAll(getMockEpsilonSet2(query));
        candidates.addAll(getMockEpsilonSet3(query));

        // Find all potential mentions
        Set<MentionCandidate> mentions = new HashSet<>();                // Seg(q) in the paper
        GreedyMentionIterator it = new GreedyMentionIterator(query);
        while (it.hasNext()) {
            MentionCandidate mention = it.next();
            mentions.add(mention);
        }

        // Calculate features for all possible pairs of <anchor, candidate entity>
        for(MentionCandidate mention : mentions) {
            for(Integer entity : candidates) {

                // Get both per-entity features and per-pair features.
                Vector<Double> entityFeatures = getEntityFeatures(entity);
                Vector<Double> mentionEntityFeatures = getMentionEntityFeatures(mention, entity);

                Vector<Double> features = new Vector<>();
                features.addAll(entityFeatures);
                features.addAll(mentionEntityFeatures);
            }
        }


        return new HashSet<>();
    }

    public String getName() {
        return "SMAPH-S annotator";
    }

}
