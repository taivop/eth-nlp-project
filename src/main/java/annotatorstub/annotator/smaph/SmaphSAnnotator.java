package annotatorstub.annotator.smaph;

import annotatorstub.annotator.FakeAnnotator;
import it.unipi.di.acube.batframework.data.ScoredAnnotation;
import it.unipi.di.acube.batframework.utils.AnnotationException;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * SMAPH-S annotator from the paper "A Piggyback System for Joint Entity Mention Detection and Linking in Web Queries".
 * @see http://www2016.net/proceedings/proceedings/p567.pdf
 */

public class SmaphSAnnotator extends FakeAnnotator {

    /**
     * Returns a set of candidate entities.
     *
     * @param query the query as a string
     * @return a set of integers, each corresponding to a Wikipedia ID of a candidate entity.
     */
    Set<Integer> epsilonSet1(String query) {
        return null;
    }

    public HashSet<ScoredAnnotation> solveSa2W(String text) throws AnnotationException {
        lastTime = System.currentTimeMillis();
        int start = 0;
        while (start < text.length() && !Character.isAlphabetic(text.charAt(start)))
            start++;
        int end = start;
        while (end < text.length() && Character.isAlphabetic(text.charAt(end)))
            end++;

        int wid;
        try {
            wid = WikipediaApiInterface.api().getIdByTitle(text.substring(start, end));
        } catch (IOException e) {
            throw new AnnotationException(e.getMessage());
        }

        HashSet<ScoredAnnotation> result = new HashSet<>();
        if (wid != -1)
            result.add(new ScoredAnnotation(start, end - start, wid, 0.1f));
        lastTime = System.currentTimeMillis() - lastTime;
        return result;
    }

    public String getName() {
        return "SMAPH-S annotator";
    }

}
