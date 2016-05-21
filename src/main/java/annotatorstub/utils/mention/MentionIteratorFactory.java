package annotatorstub.utils.mention;

import annotatorstub.utils.WATRelatednessComputer;

public class MentionIteratorFactory {
	/*
	 * returns an improved mention iterator for the query
	 * if split is true it tries to split queries by lp and not only by special chars
	 */
	public static GreedyMentionIterator getMentionIteratorForQuery(String query, boolean split) {
		/* 
		 * two problem cases identified so far:
		 * - #1 single word query with special chars like saturn/size 
		 * - #2 single word query with missing white spaces 
		 * - #3 quotation marks (fixed in GreedyMentionIterator)
		 */		
		
		/*
		 * do not modify mentions for queries:
		 * - having multiple words separated by white-spaces
		 * - having only one word of less than 6 chars
		 * - having only one word with a positive linking probability
		 */
		if (query.split(" ").length > 1 ) {
			return new GreedyMentionIterator(query);
		}
		if (query.length() <= 5 || WATRelatednessComputer.getLp(query)>0) {
			return new GreedyMentionIterator(query);
		}

		/*
		 * for #1 go through string and replace separating chars by blanks
		 * 		adam/jesus/ --> "adam jesus", "adam", "jesus"
		 */
		String altered_query = query.replaceAll("[.,~;-<>/\\|~!@#$%^&*()-_=+]"," ");
		// since we annotate not strings but ranges in the original query for instance
		// from char15 to char25 -> wiki/Obama a different length would lead to 
		// offsets from the original query and a wrong annotation.
		assert altered_query.length() == query.length();
		// was splitting successful?
		if (altered_query.split(" ").length > 1 ) {
			return new GreedyMentionIterator(altered_query);
		}
		
		/* Splitting by special chars not successful. If split is true
		 * pass query to the Iterator doing splitting by LP otherwise
		 * use the old-fashioned one which will just return one mention */
		
		if (split)
			return new GreedySplitMentionIterator(query);
		else 
			return new GreedyMentionIterator(query);
	}
}
