package annotatorstub.annotator.smaph;

import annotatorstub.annotator.tagme.TagMeAnnotator;
import it.unipi.di.acube.batframework.data.ScoredAnnotation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class to mock up some data sources to be used in the SMAPH-S annotator.
 * All the functions actually need a query argument, but we can do this in the constructor of the class later.
 */
public class SmaphSMockDataSources {

    /**
     * Returns a set of candidate entities from the output of TagMeAnnotator.
     *
     * @param query the query as a string
     * @return a set of integers, each corresponding to a Wikipedia ID of a candidate entity.
     */
    public static Set<Integer> getEpsilonSet1(String query) {
        HashSet<Integer> candidates = new HashSet<>();

        TagMeAnnotator ann = new TagMeAnnotator();
        Set<ScoredAnnotation> scoredAnnotations = ann.solveSa2W(query);

        for(ScoredAnnotation scoredAnnotation : scoredAnnotations) {
            System.out.println(scoredAnnotation.getConcept());
            candidates.add(scoredAnnotation.getConcept());
        }

        return candidates;
    }

    public static Set<Integer> getEpsilonSet2(String query) {
        return new HashSet<>();
    }

    public static Set<Integer> getEpsilonSet3(String query) {
        return new HashSet<>();
    }


    /**
     * Total total number of web pages found by Bing for query.
     * W(q) in article.
     */
    public static Long getBingTotalResults() { return 0L; }

    /**
     * List of URLs returned by Bing for query. First URL is that of the highest-rank result, etc.
     * U(q) in article.
     */
    public static List<String> getBingURLs() { return new ArrayList<>(); }

    /**
     * List of snippets returned by Bing for query. First snippet is that of the highest-rank result, etc.
     * C(q) in article.
     */
    public static List<String> getBingSnippets() { return new ArrayList<>(); }

    /**
     * The multi-set of bold portions of all snippets returned by Bing for query. Implemented as a List because we just
     * need to iterate over all elements (possibly twice if they occurred twice).
     * B(q) in article.
     */
    public static List<String> getBingBoldPortions() { return new ArrayList<>(); }




}
