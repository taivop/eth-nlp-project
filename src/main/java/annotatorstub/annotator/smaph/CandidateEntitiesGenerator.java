package annotatorstub.annotator.smaph;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import annotatorstub.utils.bing.BingResult;
import annotatorstub.utils.bing.BingWebSnippet;
import it.unipi.di.acube.batframework.data.ScoredAnnotation;
import it.unipi.di.acube.batframework.systemPlugins.WATAnnotator;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

/**
 * 
 * Implements the fetching of candidate entities based on the bing query results 
 * 
 * @author andreasgeorgiadis
 *
 */

public class CandidateEntitiesGenerator {
	
	private static WikipediaApiInterface wikipediaApiInterface;
	private static WATAnnotator watAnnotator;
	
	/**
	 * WAT annotator parameters
	 */
	private String watMethod = "salsa-auth";
	private String watEpsilon = "0.0";
	private String watSortBy = "PAGERANK";
	private String watRelatedness = "jaccard";
	private String watMinLinkProbability = "0.0";
	
	/**
	 * WAT API parameters, do not change them
	 */
	private static final String WAT_IP = "wikisense.mkapp.it";
	private static final int WAT_PORT = 80;
	
	/**
	 * Possible methods to construct the query that is fed to the
	 * WAT annotator, based on the bing search result snippets:
	 * 
	 * HIGHLIGHTED -> highlighted words are concatenated as presented in the paper. (P+ R-) => very low recall
	 * ALL -> the whole snippet is fed to the annotator. (P-- R++) => probably too crude
	 * ALL_OVERLAP -> the whole snippet is fed to the annotator, but only annotations with mentions
	 * 				  overlapping with highlighted words are kept. (P- R+) => probably the most viable
	 * 
	 * @author andreasgeorgiadis
	 *
	 */
	public static enum QueryMethod {
		HIGHLIGHTED,
		ALL,
		ALL_OVERLAP
	}
	
	public CandidateEntitiesGenerator(){
		wikipediaApiInterface = WikipediaApiInterface.api();
		watAnnotator = new WATAnnotator(WAT_IP,WAT_PORT,this.watMethod,this.watSortBy,
										this.watRelatedness,this.watEpsilon,this.watMinLinkProbability);
	}
	
	public CandidateEntitiesGenerator(String watMethod, String watEpsilon, String watSortedBy,
									  String watRelatedness, String watMinLinkProbability){
		
		this.watMethod = watMethod;
		this.watEpsilon = watEpsilon;
		this.watSortBy = watSortedBy;
		this.watRelatedness = watRelatedness;
		this.watMinLinkProbability = watMinLinkProbability;
		
		wikipediaApiInterface = WikipediaApiInterface.api();
		watAnnotator = new WATAnnotator(WAT_IP,WAT_PORT,this.watMethod,this.watSortBy,
										this.watRelatedness,this.watEpsilon,this.watMinLinkProbability);
	}
	
	/**
	 * Generates candidate entities based on the bing query search results
	 * 
	 * @param result -> bing result
	 * @param topSnippets -> number of snippets to be annotated (25 in the paper)
	 * @param queryMethod -> HIGHLIGHTED, ALL or ALL_OVERLAP
	 * @return
	 * @throws IOException
	 */
	public CandidateEntities generateCandidateEntities(BingResult result, int topSnippets, QueryMethod queryMethod) throws IOException {
		CandidateEntities bce = new CandidateEntities();
		
		Set<Integer> entitiesQuery = new HashSet<Integer>();
		Set<Integer> entitiesQueryExtended = new HashSet<Integer>();
		Set<Integer> entitiesQuerySnippetsWAT = new HashSet<Integer>();
		
		for (BingWebSnippet wikiResult : result.getWikipediaResults()) {
			String wikiTitle = wikiResult.getTitle().replace(" - Wikipedia, the free encyclopedia", "")
													.replace("- Wikipedia, the free ...", "");
			
			//discard disambiguation and list pages
			if ((!wikiTitle.toLowerCase().contains("disambiguation")) && 
				(!wikiTitle.toLowerCase().contains("list"))){
					int wikiId = wikipediaApiInterface.getIdByTitle(wikiTitle);
					entitiesQuery.add(wikiId);
			}
		}
		
		for (BingWebSnippet wikiResult : result.getExtendedWikipediaResults()) {
			String wikiTitle = wikiResult.getTitle().replace(" - Wikipedia, the free encyclopedia", "")
													.replace("- Wikipedia, the free ...", "");
			
			//discard disambiguation and list pages
			if ((!wikiTitle.toLowerCase().contains("disambiguation")) && 
				(!wikiTitle.toLowerCase().contains("list"))){
					int wikiId = wikipediaApiInterface.getIdByTitle(wikiTitle);
					entitiesQueryExtended.add(wikiId);
			}
		}
		
		int k=0;
		for (BingWebSnippet wikiResult : result.getWebResults()) {
			
			if (k++==topSnippets)
				break;
			
			String watQuery = "";
			Set<ScoredAnnotation> scoredAnnotations = new HashSet<ScoredAnnotation>();
			
			if (queryMethod.equals(QueryMethod.ALL)){
				watQuery = wikiResult.getDescription();
				scoredAnnotations = watAnnotator.solveSa2W(watQuery);
			}
			else if (queryMethod.equals(QueryMethod.HIGHLIGHTED)){
				List<String> highlightedWords = wikiResult.getHighlightedWords();
				watQuery = highlightedWords.stream().reduce("", (a,b) -> a + b);
				scoredAnnotations = watAnnotator.solveSa2W(watQuery);
			}
			else if (queryMethod.equals(QueryMethod.ALL_OVERLAP)){
				watQuery = wikiResult.getDescription();
				scoredAnnotations = watAnnotator.solveSa2W(watQuery);
				Set<ScoredAnnotation> scoredAnnotationsHighlighted = new HashSet<ScoredAnnotation>();
				
				String description = wikiResult.getDescription();
				List<String> highlightedWords = wikiResult.getHighlightedWords();
				
				Iterator<ScoredAnnotation> annotationsIter = scoredAnnotations.iterator();
				while (annotationsIter.hasNext()){
					ScoredAnnotation annotation = annotationsIter.next();
					
					boolean overlap = false;
					Iterator<String> highlightedWordsIter = highlightedWords.iterator();
					while (highlightedWordsIter.hasNext()){
						String highlightedWord = highlightedWordsIter.next();
						if (description.substring(annotation.getPosition(), annotation.getPosition() + annotation.getLength()).contains(highlightedWord)){
							overlap = true;
							break;
						}
					}
					if (overlap == true)
						scoredAnnotationsHighlighted.add(annotation);	
				}
				scoredAnnotations = scoredAnnotationsHighlighted;
			}
			entitiesQuerySnippetsWAT.addAll(scoredAnnotations.stream().map(s -> s.getConcept()).collect(Collectors.toSet()));
		}
		
		bce.setEntitiesQuery(entitiesQuery);
		bce.setEntitiesQueryExtended(entitiesQueryExtended);
		bce.setEntitiesQuerySnippetsWAT(entitiesQuerySnippetsWAT);
				
		return bce;
	}

}
