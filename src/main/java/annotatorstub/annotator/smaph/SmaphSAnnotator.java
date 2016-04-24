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

    WikipediaApiInterface wikiApi;

    public SmaphSAnnotator() throws Exception {
        this.wikiApi = WikipediaApiInterface.api();
        WATRelatednessComputer.setCache("relatedness.cache");;
    }

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


    private static int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    /**
     * Calculate Levenshtein edit distance between strings a and b.
     * @see https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
     */
    private static int ED(CharSequence a, CharSequence b) {
        int[][] distance = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++)
            distance[i][0] = i;
        for (int j = 1; j <= b.length(); j++)
            distance[0][j] = j;

        for (int i = 1; i <= a.length(); i++)
            for (int j = 1; j <= b.length(); j++)
                distance[i][j] = minimum(
                        distance[i - 1][j] + 1,
                        distance[i][j - 1] + 1,
                        distance[i - 1][j - 1] + ((a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1));

        return distance[a.length()][b.length()];
    }

    /**
     * Calculate the MinED -- a measure of distance -- as described in the paper.
     */
    private static Double minED(String a, String b) {
        String[] termsInA = a.split(" ");
        String[] termsInB = b.split(" ");

        Double minDistancesSum = 0.0;

        for(String termInA : termsInA) {
            String closestInB = "";
            Double currentMinED = Double.POSITIVE_INFINITY;

            for(String termInB : termsInB) {
                Double editDistance = (double) ED(termInA, termInB);
                if(editDistance < currentMinED) {
                    closestInB = termInB;
                    currentMinED = editDistance;
                }
            }

            minDistancesSum += currentMinED;
        }

        return minDistancesSum / termsInA.length;
    }

    /**
     * Remove a trailing parenthetical string, e.g. 'Swiss (nationality)' -> 'Swiss '.
     * @param s string to truncate
     * @return string without trailing stuff in parentheses
     */
    private static String removeFinalParentheticalString(String s) {
        int lastOpeningParenIndex = s.lastIndexOf('(');
        int lastClosingParenIndex = s.lastIndexOf(')');

        if(lastClosingParenIndex > lastOpeningParenIndex) {     // Make sure the opening parenthesis is closed.
            return s.substring(0, lastOpeningParenIndex);
        } else {
            return s;
        }
    }


    /**
     * Returns the SMAPH-S features for given entity. See table 4 in article.
     *
     * @param entity the Wikipedia ID of the entity
     * @return vector of per-entity features
     */
    private Vector<Double> getEntityFeatures(Integer entity, String query) throws IOException {
        // TODO have a cache for this so if we query the same entity again, we won't recalculate stuff.
        Vector<Double> features = new Vector<>();

        Double f4_EDTitle = minED(wikiApi.getTitlebyId(entity), query);
        Double f5_EDTitNP = minED(removeFinalParentheticalString(wikiApi.getTitlebyId(entity)), query);

        features.add(f4_EDTitle);
        features.add(f5_EDTitNP);
        features.add(1337.0);

        return features;
    }

    /**
     * Returns the SMAPH-S features for given mention-entity pair. See table 5 in article.
     *
     * @param mention the mention in query
     * @param entity the Wikipedia ID of the entity
     * @return vector of per-pair features
     */
    private Vector<Double> getMentionEntityFeatures(MentionCandidate mention, Integer entity, String query) {
        // TODO
        Vector<Double> features = new Vector<>();

        features.add(42.0);

        return features;
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
                Vector<Double> entityFeatures;
                Vector<Double> mentionEntityFeatures;
                try {
                     entityFeatures = getEntityFeatures(entity, query);
                     mentionEntityFeatures = getMentionEntityFeatures(mention, entity, query);
                } catch(IOException e) {
                    throw new AnnotationException(e.getMessage());
                }

                Vector<Double> features = new Vector<>();
                features.addAll(entityFeatures);
                features.addAll(mentionEntityFeatures);

                System.out.printf("(%s, %d) features: %s\n", mention.getMention(), entity, features);
            }
        }


        return new HashSet<>();
    }

    public String getName() {
        return "SMAPH-S annotator";
    }

}
