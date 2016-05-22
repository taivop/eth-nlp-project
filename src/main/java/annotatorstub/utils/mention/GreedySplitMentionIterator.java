package annotatorstub.utils.mention;

import java.util.ArrayList;
import java.util.List;

import annotatorstub.utils.modification.QueryProcessing;

public class GreedySplitMentionIterator extends GreedyMentionIterator {
	private List<String> mentions;
	
	public GreedySplitMentionIterator(String query) {
		super(query);
		// this class is explicitly designed for 1-word queries with missing blanks
		assert query.split(" ").length == 1;
		
		this.mentions = new ArrayList<>();
		this.mentions.add(query); // add the query-word as a possible mention
		// add all sub-words of the query
		this.mentions.addAll(QueryProcessing.getInstance()
				.splitWordInSubWords(query, query.length()-1));
	} 
	
	@Override
	public MentionCandidate next() {
		String mention = mentions.remove(0);
		int start = this.query.indexOf(mention);
		return new MentionCandidate(start, start + mention.length(), mention);
	}

	@Override
	public boolean hasNext() {
		return mentions.size()>0;
	}
}
