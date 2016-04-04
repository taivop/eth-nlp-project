package annotatorstub.annotator;

import java.util.HashSet;

import annotatorstub.utils.WATRelatednessComputer;
import annotatorstub.utils.mention.GreedyMentionIterator;
import annotatorstub.utils.mention.MentionCandidate;
import it.unipi.di.acube.batframework.data.ScoredAnnotation;
import it.unipi.di.acube.batframework.utils.AnnotationException;

/**
 * Baseline annotator as described in the project description
 */
public class BaselineAnnotator extends FakeAnnotator {

	/**
	 * Go greedily over mention candidates (see GreedyMentionIterator for
	 * details). Every mention with a linking probability > 0 gets greedily
	 * linked to the Wikipedia article with the highest commonness.
	 */
	@Override
	public HashSet<ScoredAnnotation> solveSa2W(String text) throws AnnotationException {
		lastTime = System.currentTimeMillis();
		// the found and scored annotations:
		HashSet<ScoredAnnotation> scoredAnnotations = new HashSet<ScoredAnnotation>();
		// do not link anything that is a substring of this set
		// if "Queen Elizabeth" has been linked don't link "Queen" or
		// "Elizabeth"
		
		HashSet<String> usedMentions = new HashSet<String>();

		GreedyMentionIterator it = new GreedyMentionIterator(text);
		while (it.hasNext()) {
			MentionCandidate candidate = it.next();
			String mention = candidate.getMention();
			
			if (mention.length()<2) {
				continue;
			}
			
			if (!usedMentions.stream().anyMatch(m -> m.contains(mention))) {
				double mentionProbability = WATRelatednessComputer.getLp(mention);
				if (mentionProbability > 0) {
					usedMentions.add(mention);
					// find Wikipedia article with highest commonness:
					double highestScore = -1.0;
					int wikiId = -1;
					for (int id : WATRelatednessComputer.getLinks(mention)) {
						double commonness = WATRelatednessComputer.getCommonness("obama", id);
						if (commonness > highestScore) {
							highestScore = commonness;
							wikiId = id;
						}
					}
					if (candidate.getQueryStartPosition() < 0) {
						System.out.println("break");
					}
					scoredAnnotations
							/*TODO-Bernhard: what is the last parameter in ScoredAnnotation for?*/
							.add(new ScoredAnnotation(candidate.getQueryStartPosition(), mention.length(), wikiId, 0.1f) 
					);
				}
			}
		}
		lastTime = System.currentTimeMillis() - lastTime;
		return scoredAnnotations;
	}

	@Override
	public String getName() {
		return "Simple baseline-1 annotator";
	}
}
