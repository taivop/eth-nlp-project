package annotatorstub.utils.bing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import it.unipi.di.acube.BingInterface;

/**
 * Singleton API for bing search
 */
public class BingSearchAPI {

	private BingInterface bing = null;
	private Pattern highlightPattern = Pattern.compile("\\uE000([^\\uE000]*)\\uE001");

	private static BingSearchAPI INSTANCE = null;
	public static String KEY = null;

	public static BingSearchAPI getInstance() {
		if (INSTANCE == null) {
			if (KEY.isEmpty()) {
				System.err.println("BingSearchAPI: no key found.");
				return null;
			}
			INSTANCE = new BingSearchAPI();
		}
		return INSTANCE;
	}

	private BingSearchAPI() {
		assert KEY != null;
		bing = new BingInterface(KEY);
		try {
			BingInterface.setCache("bing.cache");
			System.out.println("Using bing query cache.");
		} catch (ClassNotFoundException | IOException e) {
			System.err.println("BingSearchAPI: error on loading bing.cache: " + e.getMessage());
		}
	}

	/**
	 * 1) queries for the queryString 2) does an extended query for queryString
	 * + " wikipedia"
	 * 
	 * @see annotatorstub.utils.bing.BingResult for more info
	 */
	public BingResult query(String queryString) throws Exception {
		JSONObject bingQueryResult = bing.queryBing(queryString);
		JSONObject bingExtendedQueryResult = bing.queryBing(queryString + " wikipedia");
		BingInterface.flush();
		JSONObject result = bingQueryResult.getJSONObject("d").getJSONArray("results").getJSONObject(0);

		Long webTotal = result.getLong("WebTotal");

		// general information
		String alteredQueryString = removeHighlighting(result.getString("AlteredQuery"));

		// parse the web results
		JSONArray bingWebSnippetsJ = result.getJSONArray("Web");
		List<BingWebSnippet> bingWebSnippets = new ArrayList<BingWebSnippet>();
		for (int i = 0; i < bingWebSnippetsJ.length(); i++) {
			bingWebSnippets.add(parseWebSnippet(bingWebSnippetsJ.getJSONObject(i)));
		}

		// parse related searches
		JSONArray relatedSearchJ = result.getJSONArray("RelatedSearch");
		List<String> relatedSearchTitles = new ArrayList<String>();
		for (int i = 0; i < relatedSearchJ.length(); i++) {
			relatedSearchTitles.add(relatedSearchJ.getJSONObject(i).getString("Title"));
		}

		// parse spelling suggestions
		JSONArray spellingSuggestionsJ = result.getJSONArray("SpellingSuggestions");
		List<String> spellingSuggestions = new ArrayList<String>();
		for (int i = 0; i < spellingSuggestionsJ.length(); i++) {
			spellingSuggestions.add(spellingSuggestionsJ.getJSONObject(i).getString("Value"));
		}

		// get wikipedia links for the extended query
		JSONArray bingExtendedWebSnippetsJ = bingExtendedQueryResult.getJSONObject("d").getJSONArray("results")
				.getJSONObject(0).getJSONArray("Web");
		List<BingWebSnippet> bingExtendedWebSnippets = new ArrayList<BingWebSnippet>();
		for (int i = 0; i < bingExtendedWebSnippetsJ.length(); i++) {
			BingWebSnippet snippet = parseWebSnippet(bingExtendedWebSnippetsJ.getJSONObject(i));
			if (snippet.isWikipedia()) {
				bingExtendedWebSnippets.add(snippet);
			}
		}

		return new BingResult(queryString, alteredQueryString, spellingSuggestions, relatedSearchTitles,
				bingWebSnippets, bingExtendedWebSnippets, webTotal);
	}

	//////////////////
	// private methods
	//////////////////

	private String removeHighlighting(String text) {
		return text.replace("\uE000", "").replace("\uE001", "");
	}

	private List<String> getHighlightedWords(String text) {
		List<String> result = new ArrayList<String>();
		Matcher matcher = highlightPattern.matcher(text);
		while (matcher.find()) {
			result.add(removeHighlighting(matcher.group()));
		}
		return result;
	}

	private BingWebSnippet parseWebSnippet(JSONObject snippet) throws JSONException {
		String title = removeHighlighting(snippet.getString("Title"));
		String description = removeHighlighting(snippet.getString("Description"));
		String url = snippet.getString("Url");
		List<String> highlightedWords = getHighlightedWords(snippet.getString("Description"));
		boolean isWikipediaLink = url.matches("^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]{2,3}wikipedia[.]org/.+");
		return new BingWebSnippet(title, description, url, highlightedWords, isWikipediaLink);
	}
}
