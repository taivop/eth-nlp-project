package annotatorstub.annotator.tagme;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import annotatorstub.annotator.FakeAnnotator;
import annotatorstub.utils.WATRelatednessComputer;
import annotatorstub.utils.mention.GreedyMentionIterator;
import annotatorstub.utils.mention.MentionCandidate;
import it.unipi.di.acube.batframework.data.ScoredAnnotation;
import it.unipi.di.acube.batframework.utils.AnnotationException;

/**
 * Implementation of the TagMe Annotator as described in the paper by Paolo
 * Ferragina and Ugo Scaiella
 * 
 * recall this has been designed for annotating search engine results, tweets
 * etc. not necessarily very short queries
 * 
 * @see http://pages.di.unipi.it/ferragina/cikm2010.pdf
 * @see http://tagme.di.unipi.it
 */
public class TagMeAnnotator extends FakeAnnotator {
	
	private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/*
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * 
	 * Tag-ME PARAMETERS
	 * please document if you change any parameter, especially why
	 * and if you train them which data set/method has been used to do so
	 * Do not use the Benchmark scores on the TEST-Set to change any of those
	 * parameters use TrainA and validate with TrainB instead!
	 * 
	 * (2016-04-11) 
	 * TAU = 0.02 as described in paper
	 * EPSILON = 0.3 as described in paper
	 * PRUNING_THRESHOLD = 0.15 no description in paper just set by intuition 
	 * 
	 * TODO: maybe some proper tuning would help - especially once we 
	 * use it with bing results instead of the small queries
	 */
	
	/**
	 * To speed up things and eliminate the number of pairwise relatedness 
	 * calculations we only consider senses which prior (commonness) is bigger
	 * or equal than TAU. 
	 * 
	 * assert 0 < TAU < 1 
	 */
	private final double TAU = 0.02;

	/**
	 * We chose to implement Disambiguation by Threshold (DT):
	 * The top-EPSILON best senses for each anchor are kept. From those we 
	 * select the one with highest commonness for annotating the anchor.
	 * 
	 * assert 0 < EPSILON < 1 
	 */
	private final double EPSILON = 0.3;
	
	/**
	 * In the pruning step we calculate the average of the following
	 * two features for each annotation: 
	 *    * linking probability of the anchor to the candidate sense
	 *    * average relatedness between the candidate sense and all 
	 *      other candidate senses
	 * if the average is above this threshold we keep the annotation
	 * otherwise we prune it.
	 * 
	 * This parameter is used for precision - recall tuning.
	 * A high threshold means high precision but low recall and vice
	 * versa.
	 */
	private final double PRUNING_THRESHOLD = 0.15;
	
	/**
	 * Minimum link probability of candidate anchor. Used during
	 * the parsing phase in order to reduce the search space.
	 */
	private final double LP_THRESHOLD = 0.1;
	
	/**
	 * Minimum linking frequency of candidate anchor. Used during
	 * the parsing phase in order to reduce the search space.
	 */
	private final double LINK_FREQUENCY_THRESHOLD = 2;
	
	/**
	 * Anchors returning error 500 when wikipedia api is queried
	 */
	private List<String> anchorBlackList;
	
	public TagMeAnnotator() {
		anchorBlackList = new LinkedList<String>();
		
		try {
			Scanner in = new Scanner(new FileReader("anchor_black_list.txt"));
			while (in.hasNext())
				anchorBlackList.add(in.next());
			in.close();
		}
		catch (FileNotFoundException e){
			logger.warn("Anchor black list file not found");
		}
	}

	/*
	 * end of parameters.
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	/*
	 *  helper method to sort a hash map by its values.
	 *  @see http://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java
	 */
	private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		Map<K, V> result = new LinkedHashMap<>();
		Stream<Entry<K, V>> st = map.entrySet().stream();
		st.sorted(Comparator.comparing(e -> e.getValue())).forEachOrdered(e -> result.put(e.getKey(), e.getValue()));
		return result;
	}

	/**
	 * Returns the set of anchors for a given query (there might be overlapping
	 * anchors). Therefore the system looks at every possible substring 
	 * and checks weather it has a linking probability bigger than zero.  
	 * 
	 * @param query the query as a string
	 * @return a set of mention candidates 
	 */
	private Set<MentionCandidate> getAnchorsFromText(String query) {
		/*
		 * TODO: for search engine results the greedy iteration returns 
		 * quite a lot of long and therefore not necessarily relevant mentions
		 * which will end up with lp of zero. It might be wise to consider only
		 * 3-Grams, 2-Grams and single words for runtime reasons.
		 */
		//TODO check strip characters
		query = query.toLowerCase();
		query = query.replaceAll("[.,~;\"-<>/\\|~!@#$%^&*()-_=+]","");
				
		GreedyMentionIterator it = new GreedyMentionIterator(query);
		
		Set<MentionCandidate> anchors = new HashSet<>();
		while (it.hasNext()) {
			MentionCandidate anchor = it.next();
			/* TODO the following if just disregards mentions of less equal one
			 * char because a "?" query to wiki-sense caused an exception  we use
			 * the same if in the baseline annotator - take that into account when 
			 * changing to N-Grams (TODO above)
			 * 
			 * Also discards badly behaving (blacklisted) anchors
			 */
			if ((anchor.getMention().length() > 1) && (!anchorBlackList.contains(anchor.getMention()))) {
				if ((WATRelatednessComputer.getLp(anchor.getMention()) > LP_THRESHOLD) &&
					(WATRelatednessComputer.getLinks(anchor.getMention()).length > LINK_FREQUENCY_THRESHOLD))
					anchors.add(anchor);
			}
		}
		return anchors;
	}

	/**
	 * Returns a set of Wikipedia article IDs which are possibly linked by the given 
	 * anchor having a prior probability bigger equal to TAU.
	 * 
	 * @param anchor the anchor which possibly refers to a set of articles
	 * @return the set of articles which are relevant for this anchor
	 */
	private Set<Integer> relevantPagesLinkedBy(String anchor) {
		int[] original = WATRelatednessComputer.getLinks(anchor);
		Set<Integer> probLinks = new HashSet<>();
		for (int wikiID : original) {
			if (WATRelatednessComputer.getCommonness(anchor, wikiID) >= TAU) {
				probLinks.add(wikiID);
			}
		}
		return probLinks;
	}

	/**
	 * The disambiguation step as described in the paper finds one Wikipedia article 
	 * for every anchor. This is done in two steps:
	 *    1) in a voting process all anchors vote (collectiveAgreements) for the possible 
	 *       candidates of other anchors
	 *    2) in a second step we select the one candidate with the highest commonness out
	 *    	 of the epsilon-top candidates from step 1
	 */
	private HashSet<ScoredAnnotation> disambiguateMentions(Set<MentionCandidate> anchors)  {
		/* collective agreements 
		 * MAP [ anchor ---> MAP [ wikipedia-article-id ---> score ] ]
		 * maps all anchors in the input query to a map containing all relevant wiki pages and
		 * their score - initialized with zero
		 * 
		 * not that after the following initialization we get the relevant pages for anchor a by: 
		 * collectiveAgreements.get(a).keySet()
		 */
		Map<MentionCandidate, Map<Integer, Double>> collectiveAgreements = new HashMap<>();
		for (MentionCandidate a : anchors) {
			collectiveAgreements.put(a, new HashMap<Integer, Double>());
			Set<Integer> pagesLinkedByA = this.relevantPagesLinkedBy(a.getMention());
			for (int wikiPage : pagesLinkedByA) {
				collectiveAgreements.get(a).put(wikiPage, 0.0);
			}
		}

		/* * * *  STEP 1 * * * */ 
		
		for (MentionCandidate a : anchors) {
			for (MentionCandidate b : anchors) {
				// b doesn't vote for itself:
				if (a.equals(b)) continue;
				// b votes for each candidate of a separately:
				for (int candiateA : collectiveAgreements.get(a).keySet()) {
					double voteBforCandidateA = 0.0;
					for (int candidateB : collectiveAgreements.get(b).keySet()) {
						voteBforCandidateA += (WATRelatednessComputer.getMwRelatedness(candiateA, candidateB)
								* WATRelatednessComputer.getCommonness(b.getMention(), candidateB));
					}
					// normalize:
					voteBforCandidateA /= (collectiveAgreements.get(b).keySet().size()); 
					// update votes:
					collectiveAgreements.get(a).put(candiateA,
							collectiveAgreements.get(a).get(candiateA) + voteBforCandidateA);
				}
			}
		}
		
		/* * * *  STEP 2 * * * */ 

		HashSet<ScoredAnnotation> scoredAnnotations = new HashSet<ScoredAnnotation>();
		for (MentionCandidate a : anchors) {
			// sort the ratings for the candidates of a by value from low to high:
			Map<Integer, Double> sortedRatings = sortByValue(collectiveAgreements.get(a));
			int numberOfCandidates = sortedRatings.size(); // clean up

			int i = 0;
			double maxScore = Double.MIN_VALUE; // best commonness score so far
			int bestFit = -1; // wikipedia id with best commonness score so far

			for (Entry<Integer, Double> entry : sortedRatings.entrySet()) {
				i++;
				// skip first part of the list we only consider the top-EPSLION-percent
				// AND check if score is better than old score 
				if ((i >= numberOfCandidates - Math.round(numberOfCandidates * EPSILON)) &&
						WATRelatednessComputer.getCommonness(a.getMention(), entry.getKey()) > maxScore) {
					maxScore = WATRelatednessComputer.getCommonness(a.getMention(), entry.getKey());
					bestFit = entry.getKey();
				}
			}
			// finally add the annotation (we do no pruning here):
			scoredAnnotations.add(new ScoredAnnotation(a.getQueryStartPosition(), a.getMention().length(), bestFit,
					(float) maxScore));
		}
		return scoredAnnotations;
	}

	/**
	 * Prunes the annotation of the disambiguation step. We consider two 
	 * features for each annotation:
	 *    * linking probability of the anchor to the candidate sense
	 *    * average relatedness between the candidate sense and all 
	 *      other candidate senses
	 * We keep annotations where the average of both features is larger
	 * than a threshold (PRUNING_THRESHOLD)
	 */
	private Set<ScoredAnnotation> pruneAnnotations(Set<ScoredAnnotation> annotations, String query) {
		Set<ScoredAnnotation> finalAnnotations = new HashSet<>();
		
		for (ScoredAnnotation annotation : annotations) {
			
			double averageRelatedness = 0.0;
			for (ScoredAnnotation otherAnnotation : annotations) {
				if (!annotation.equals(otherAnnotation)) {
					averageRelatedness += WATRelatednessComputer.getMwRelatedness(annotation.getConcept(),
							otherAnnotation.getConcept());
				}
			}
			averageRelatedness /= (annotations.size() - 1);
			
			String annotatedString = query.substring(annotation.getPosition(),
					annotation.getPosition() + annotation.getLength());
			double linkingProbability = WATRelatednessComputer.getLp(annotatedString);
			 
			if ((averageRelatedness+linkingProbability) > PRUNING_THRESHOLD) {
				finalAnnotations.add(annotation);
			}
		}
		return finalAnnotations;
	}
	
	
	@Override
	public HashSet<ScoredAnnotation> solveSa2W(String text) throws AnnotationException {
		return (HashSet<ScoredAnnotation>) pruneAnnotations(disambiguateMentions(getAnchorsFromText(text)),text);
	}
	
	
	@Override
	public String getName() {
		return "Tag-ME annotator";
	}
}
