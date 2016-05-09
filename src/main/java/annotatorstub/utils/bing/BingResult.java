package annotatorstub.utils.bing;

import java.util.List;
import java.util.stream.Collectors;

public class BingResult {

	/**
	 * original query string
	 */
	private String queryString;

	/**
	 * indicates weather the original query has been altered by bing (spelling
	 * correction)
	 */
	private boolean altered;

	/**
	 * altered query string (by bing) if altered = false - this is equal to
	 * query
	 */
	private String alteredQueryString;

	/**
	 * a list with spelling suggestions for the queryString
	 */
	private List<String> spellingSuggestions;

	/**
	 * a list of related bing queries for this query
	 */
	private List<String> relatedSearchTitles;

	/**
	 * ordered results of the web search there are different getters for this
	 * list which filter for Wikipedia articles and so on
	 */
	private List<BingWebSnippet> webResults;

	/**
	 * we extend the query in the following way: "original query string" +
	 * " wikipedia" this list contains all BingWebSnippets for the extended
	 * query which link to an Wikipedia article
	 */
	private List<BingWebSnippet> extendedWikipediaResults;

	/**
	 * number of results in query
	 */
	private Long webTotal;

	public BingResult(String queryString, String alteredQueryString, List<String> spellingSuggestions,
			List<String> relatedSearchTitles, List<BingWebSnippet> webResults,
			List<BingWebSnippet> extendedWikipediaResults, Long webTotal) {
		super();
		this.queryString = queryString;
		if (alteredQueryString.isEmpty()) {
			this.alteredQueryString = queryString;
			this.altered = false;
		} else {
			this.alteredQueryString = alteredQueryString;
			this.altered = true;
		}
		this.spellingSuggestions = spellingSuggestions;
		this.relatedSearchTitles = relatedSearchTitles;
		this.webResults = webResults;
		this.extendedWikipediaResults = extendedWikipediaResults;
		this.webTotal = webTotal;
	}

	public String getQueryString() {
		return queryString;
	}

	public boolean isAltered() {
		return altered;
	}

	public String getAlteredQueryString() {
		return alteredQueryString;
	}

	public List<String> getSpellingSuggestions() {
		return spellingSuggestions;
	}

	public List<String> getRelatedSearchTitles() {
		return relatedSearchTitles;
	}

	public List<BingWebSnippet> getWikipediaResults() {
		return webResults.stream().filter(result -> result.isWikipedia()).collect(Collectors.toList());
	}

	public List<BingWebSnippet> getNonWikipediaResults() {
		return webResults.stream().filter(result -> !result.isWikipedia()).collect(Collectors.toList());
	}

	public List<BingWebSnippet> getWebResults() {
		return webResults;
	}

	/**
	 * CAUTION: for this list the original query has been modified by putting
	 * "wikipedia" in the end this list contains only the results of the
	 * modified query which are linking to wikipedia pages.
	 */
	public List<BingWebSnippet> getExtendedWikipediaResults() {
		return extendedWikipediaResults;
	}

	public Long getWebTotal() {
		return webTotal;
	}
}
