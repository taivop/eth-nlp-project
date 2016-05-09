package annotatorstub.annotator.smaph;

import it.unipi.di.acube.batframework.data.ScoredAnnotation;

import java.util.List;
import java.util.Set;

/**
 * Wrapper class representing candidate entity sets (E1, E2 and E3) for SMAPH annotators
 * 
 * @author andreasgeorgiadis
 */

public class CandidateEntities {
	
	/**
	 * entities that correspond to the wikipedia pages
	 * that were returned by the original query
	 */
	private Set<Integer> entitiesQuery;
	
	/**
	 * entities that correspond to the wikipedia pages
	 * that were returned by the extended query (+ " wikipedia")
	 */
	private Set<Integer> entitiesQueryExtended;
	
	/**
	 * entities returned after annotating the web search
	 * results snippets using the TAGME annotator
	 */
	private Set<Integer> entitiesQuerySnippetsWAT;

	/**
	 * same as entitiesQuerySnippetsWAT, but broken down by snippet, i.e. the i-th of the list contains the set of
	 * wikipedia pages found in the i-th snippet
	 */
	private List<Set<Integer>> entitiesQuerySnippetsWATBySnippet;

	/**
	 * the annotations found for each snippet in the Bing results
	 */
	private List<Set<ScoredAnnotation>> WATSnippetAnnotations;

	public Set<Integer> getEntitiesQuery() {
		return entitiesQuery;
	}

	public void setEntitiesQuery(Set<Integer> entitiesQuery) {
		this.entitiesQuery = entitiesQuery;
	}

	public Set<Integer> getEntitiesQueryExtended() {
		return entitiesQueryExtended;
	}

	public void setEntitiesQueryExtended(Set<Integer> entitiesQueryExtended) {
		this.entitiesQueryExtended = entitiesQueryExtended;
	}

	public Set<Integer> getEntitiesQuerySnippetsWAT() {
		return entitiesQuerySnippetsWAT;
	}

	public void setEntitiesQuerySnippetsWAT(Set<Integer> entitiesQuerySnippetsWAT) {
		this.entitiesQuerySnippetsWAT = entitiesQuerySnippetsWAT;
	}

	public List<Set<Integer>> getEntitiesQuerySnippetsWATBySnippet() { return entitiesQuerySnippetsWATBySnippet; }

	public void setEntitiesQuerySnippetsWATBySnippet(List<Set<Integer>> entitiesQuerySnippetsWATBySnippet) {
		this.entitiesQuerySnippetsWATBySnippet = entitiesQuerySnippetsWATBySnippet;
	}

	public List<Set<ScoredAnnotation>> getWATSnippetAnnotations() { return WATSnippetAnnotations; }

	public void setWATSnippetAnnotations(List<Set<ScoredAnnotation>> WATSnippetAnnotations) {
		this.WATSnippetAnnotations = WATSnippetAnnotations;
	}
}
