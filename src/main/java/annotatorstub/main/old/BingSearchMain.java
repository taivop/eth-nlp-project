package annotatorstub.main.old;

import java.util.Iterator;
import java.util.Set;

import annotatorstub.annotator.smaph.CandidateEntities;
import annotatorstub.annotator.smaph.CandidateEntitiesGenerator;
import annotatorstub.utils.bing.BingResult;
import annotatorstub.utils.bing.BingSearchAPI;
import annotatorstub.utils.bing.BingWebSnippet;

public class BingSearchMain {
	
	public static void main(String[] args) throws Exception {
		// YOU HAVE TO DO THIS ONLY ONCE:
		BingSearchAPI.KEY = "crECheFN9wPg0oAJWRZM7nfuJ69ETJhMzxXXjchNMSM";
		/////////////////////////////////

		/*
		 * caching is enabled by default the raw jason result for each query
		 * will be added to the bing.cache file in ./ to empty the cache delete
		 * this file (cache is not part of the git repo!)
		 */

		// API is a singleton - set the key once then use .getInstance() to work
		// with the API
		BingSearchAPI bingApi = BingSearchAPI.getInstance();

		// the type-o in the query is on purpose
		String queryName = "Armstrong Moon lading";
		BingResult result = bingApi.query("Armstrong Moon lading");
		System.out.println("\n\t-\t-\t-\t-");
		System.out.println("Results for " + queryName);
		if (result.isAltered()) {
			System.out.println("> The query has been alterd by bing to: " + result.getAlteredQueryString());
		}
		System.out.println("> Related querys on bing are: ");
		for (String related : result.getRelatedSearchTitles()) {
			System.out.println("\t > " + related);
		}
		System.out.println("> Spelling suggestions by bing are");
		for (String suggestion : result.getSpellingSuggestions()) {
			System.out.println("\t > " + suggestion);
		}
		System.out.println("> Web search results which are wikipedia pages:");
		for (BingWebSnippet wikiResult : result.getWikipediaResults()) {
			System.out.println("\t > " + wikiResult.getTitle());
			System.out.println("\t Higlighted words in description: > " + wikiResult.getHighlightedWords().toString());
			System.out.println("\t Full description: > " + wikiResult.getDescription());

		}
		System.out.println("> Top 3 search results which are NOT wikipedia pages:");
		int i = 0;
		for (BingWebSnippet nonWikiResult : result.getNonWikipediaResults()) {
			System.out.println("\t > " + nonWikiResult.getTitle());
			System.out
					.println("\t Higlighted words in description: > " + nonWikiResult.getHighlightedWords().toString());
			System.out.println("\t Full description: > " + nonWikiResult.getDescription());
			i++;
			if (i == 3)
				break;
		}
		System.out.println("> Extended web search (add 'wikipedia' to query) results:");
		for (BingWebSnippet wikiResult : result.getExtendedWikipediaResults()) {
			System.out.println("\t > " + wikiResult.getTitle());
			System.out.println("\t Higlighted words in description: > " + wikiResult.getHighlightedWords().toString());
			System.out.println("\t Full description: > " + wikiResult.getDescription());

		}
		
	}
}
