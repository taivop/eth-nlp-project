package annotatorstub.utils.bing;

import java.util.List;

/**
 * A short summary of a bing web result
 */
public class BingWebSnippet {

	/**
	 * title without any highlighting characters
	 */
	private String title;

	/**
	 * description without any highlighting characters
	 */
	private String description;

	/**
	 * URL of result
	 */
	private String url;

	/**
	 * All highlighted words (highlighting is done by bing) in the description
	 * of the web result
	 */
	private List<String> highlightedWords;

	/**
	 * marks weather this snippet links to a Wikipedia page or not
	 */
	private boolean isWikipediaLink;

	public BingWebSnippet(String title, String description, String url, List<String> highlightedWords,
			boolean isWikipediaLink) {
		super();
		this.title = title;
		this.description = description;
		this.url = url;
		this.highlightedWords = highlightedWords;
		this.isWikipediaLink = isWikipediaLink;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getUrl() {
		return url;
	}

	public List<String> getHighlightedWords() {
		return highlightedWords;
	}

	public boolean isWikipedia() {
		return isWikipediaLink;
	}

}
