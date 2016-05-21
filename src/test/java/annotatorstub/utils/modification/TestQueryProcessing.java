package annotatorstub.utils.modification;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import annotatorstub.utils.WATRelatednessComputer;
import annotatorstub.utils.bing.BingSearchAPI;
import annotatorstub.utils.mention.GreedyMentionIterator;
import annotatorstub.utils.mention.MentionCandidate;
import it.unipi.di.acube.batframework.datasetPlugins.DatasetBuilder;
import it.unipi.di.acube.batframework.datasetPlugins.GERDAQDataset;

public class TestQueryProcessing {
	
	
	private static List<String> getModifications(GERDAQDataset dataset, QueryProcessing instance) {
		List<String> changes  = new ArrayList<>();
		for (String query : dataset.getTextInstanceList()) {
			String altered = instance.alterQueryForBingSearch(query);
			if (query.split(" ").length==1 && !query.equals(altered)) {
				changes.add(query+" -> "+altered);
			}
		}
		return changes;
	}
	
	public static void main (String[] args) throws FileNotFoundException, ClassNotFoundException, IOException {
		WATRelatednessComputer.setCache("relatedness.cache");
		BingSearchAPI.KEY = "+MkADwXpOGeryP7sNqlkbtUeZYhs8mUeUBsNq++Yk1U";
		QueryProcessing instance = QueryProcessing.getInstance();		

		
		List<String> modifications = getModifications(DatasetBuilder.getGerdaqTrainA(), instance);
		modifications.addAll(getModifications(DatasetBuilder.getGerdaqTrainB(), instance));
		modifications.addAll(getModifications(DatasetBuilder.getGerdaqDevel(), instance));
		modifications.addAll(getModifications(DatasetBuilder.getGerdaqTest(), instance));
	}
}
