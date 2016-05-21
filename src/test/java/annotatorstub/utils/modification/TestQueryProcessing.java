package annotatorstub.utils.modification;

import java.io.FileNotFoundException;
import java.io.IOException;

import annotatorstub.utils.WATRelatednessComputer;
import annotatorstub.utils.bing.BingSearchAPI;

public class TestQueryProcessing {
	public static void main (String[] args) throws Exception {
		WATRelatednessComputer.setCache("relatedness.cache");
		BingSearchAPI.KEY = "+MkADwXpOGeryP7sNqlkbtUeZYhs8mUeUBsNq++Yk1U";
		QueryProcessing instance = QueryProcessing.getInstance();
		
//		String query = "north20carolina20in20aolmsnearthlinknetscapesympat";
		String query = "how29isbabbyformed";

		String newQuery = instance.alterQueryForBingSearch(query);
		
		System.out.println("old query: "+query);
		System.out.println("new query: "+newQuery);
		
	}
}
