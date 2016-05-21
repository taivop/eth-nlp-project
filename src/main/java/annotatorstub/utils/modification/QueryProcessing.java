package annotatorstub.utils.modification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import annotatorstub.utils.WATRelatednessComputer;
import annotatorstub.utils.bing.BingSearchAPI;
import it.unipi.di.acube.batframework.utils.AnnotationException;

/*
 * Singleton query processor
 */
public class QueryProcessing {
	
	private BingSearchAPI bingApi;
	private static QueryProcessing INSTANCE = null;

	public static QueryProcessing getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new QueryProcessing();
		}
		return INSTANCE;
	}
	
	private QueryProcessing() {
		// since bing-api and WAT cache should be initialized by 
		// smaph-* check this here.
        bingApi = BingSearchAPI.getInstance();
        if (bingApi == null) {
        	throw new AnnotationException("no bing-key was set before calling the query processor");
        }
        
        try {
        	WATRelatednessComputer.flush();
        } catch (IOException e) {
        	throw new AnnotationException("no WAT chache file was loaded before calling the query processor");
		}
	}
	
	public String alterAndSplitQuery(String query) throws Exception {
		query = bingApi.query(query).getAlteredQueryString();
		
		/*
		 *  if the query contains more than one word leave it as is
		 */
		if (query.split(" ").length > 1) {
			return query;
		}
		
		/*
		 * empirical threshold.. 
		 * average English word length is 5 chars do not split
		 * smaller words. 
		 */
		if (query.length() <= 5) {
			return query;
		}
		
		/*
		 * check if word could actually make sense ( has a positive
		 * linking probability in wikipedia) if yes do not change it
		 */
		if (WATRelatednessComputer.getLp(query)>0) {
			return query;
		}

		/*
		 * try splitting by special chars and digits
		 */
		query = query.replaceAll("[.,~;\"-<>/\\|~!@#$%^&*()-_=+]"," ");
		query = query.replaceAll("[0-9]"," ");
		query = query.trim();
		if (query.split(" ").length > 1) {
			String modified = "";
			for (String word : query.split(" ")) {
				if (word.isEmpty()) {
					continue;
				}
				word = word.trim();
				if (word.length()>2 && WATRelatednessComputer.getLp(query)>0) {
					modified += word+" ";
				} else if (word.length()>2) {
					// try to split the word further
					modified += alterAndSplitQuery(word)+" ";
				}
			}
			return modified.trim();
		}
		
		/*
		 * try to greedily find the biggest part of the word with a 
		 * positive linking probability.  
		 */
		List<String> result = split(query, query.length()-1);
		
		/* 
		 * reconstruct query - do not only keep the found sub-words but split the original 
		 * query with blanks at the beginning of found sub-words.
		 */
		String query_new = "";
		for (int i=0; i<result.size(); i++) {
			if (i == result.size()-1) {
				query_new += query.substring(query.indexOf(result.get(i)));
			} else {
				int currentStart = query.indexOf(result.get(i));
				int nextStart = query.indexOf(result.get(i+1));
				query_new += query.substring(currentStart, nextStart)+ " ";
			}
		}		
		return query_new.trim();
	}
	
	/*
	 * recursive method for word splitting
	 * @param query the query to split
	 * @param maxWindowSize the max length of sub-words to search in the original word 
	 * @return list of words found in query (ordered!)
	 */
	private List<String> split(String query,  int maxWindowSize) {
		List<String> found = new ArrayList<>();
		// check if substring is smaller than window size
		if ((query.length() - 1) < maxWindowSize) {
			maxWindowSize = (query.length() - 1);
		}
		for (int i=maxWindowSize; i>2; i--) {
			for (int j=0; j<=(query.length()-i); j++) {
				String substring = query.substring(j, j+i);
				if (WATRelatednessComputer.getLp(substring) > 0) {
					// keep query ordering!
					// first left substrings
					String leftSubstring = query.substring(0, j);
					if (leftSubstring.length() > 2) {
						found.addAll(split(leftSubstring,i-1));
					}
					// then the found one
					found.add(substring);
					// then right substrings
					String rightSubstring = query.substring(j+i);
					if (rightSubstring.length() > 2) {
						found.addAll(split(rightSubstring,i));
					}
					return found;
				}
			}
		}
		return found;	
	}
}
