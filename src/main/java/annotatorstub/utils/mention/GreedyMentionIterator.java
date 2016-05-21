package annotatorstub.utils.mention;

import java.util.*;

/**
 * Iterate over a query and extract mention candidates (n-Shingles) greedily:
 *  - longest n-Shingles first
 *  - shingles left in query before shingles right in the query
 */
public class GreedyMentionIterator implements Iterator<MentionCandidate> {
	private String[] words; // query split in words
	protected String query; // original query
	protected int pos = 0;
	private int n; // current shingle size
	protected boolean hasNext = true;

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
		
		/* check if mention ends with " but does not start with one exclude " 
		 * from the mention  (vice versa with starting and not ending)
		 * This happens in queries like >"..... Champagne"< to generate the 
		 * mention >Champagne"< does not make sense.
		 */
		if (mention.endsWith("\"") && !mention.startsWith("\"")) {
			mention = mention.substring(0, mention.length()-1);
		}
		if (mention.startsWith("\"") && !mention.endsWith("\"")) {
			mention = mention.substring(1, mention.length());
			start++;
		}
		/* because of the  injected white spaces instead of special chars
		 * it can happen that the mention consists only of one char which does
		 * not make sense.. try to avoid this cases.
		 * Only if the last mention has 1 char return it .... 
		 * return null in this case would cause a lot of side effects*/
		if (mention.length() < 2 && hasNext) {
			return next();
		}  else {
			return new MentionCandidate(start, start + mention.length(), mention);
		}
	}
}
