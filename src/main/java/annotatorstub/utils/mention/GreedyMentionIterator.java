package annotatorstub.utils.mention;

import java.util.*;

/**
 * Iterate over a query and extract mention candidates (n-Shingles) greedily:
 *  - longest n-Shingles first
 *  - shingles left in query before shingles right in the query
 */
public class GreedyMentionIterator implements Iterator<MentionCandidate> {
	private String[] words; // query split in words
	private String query; // original query
	private int pos = 0;
	private int n; // current shingle size
	private boolean hasNext = true;

	public GreedyMentionIterator(String query) {
		this.query = query;
		this.words = query.split(" ");
		this.n = words.length;
	}

	public boolean hasNext() {
		return hasNext;
	}

	public MentionCandidate next() {
		StringBuilder sb = new StringBuilder();
		for (int i = pos; i < pos + n; i++) {
			sb.append((i > pos ? " " : "") + words[i]);
		}
		if (++pos >= (words.length - n + 1)) {
			n--;
			pos = 0;
			if (n == 0) {
				hasNext = false;
			}
		}
		String mention = sb.toString();
		int start = query.indexOf(mention);
		return new MentionCandidate(start, start + mention.length(), mention);
	}
}
