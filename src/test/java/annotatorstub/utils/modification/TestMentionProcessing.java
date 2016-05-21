package annotatorstub.utils.modification;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import annotatorstub.utils.WATRelatednessComputer;
import annotatorstub.utils.bing.BingSearchAPI;
import annotatorstub.utils.mention.GreedyMentionIterator;
import annotatorstub.utils.mention.MentionCandidate;
import annotatorstub.utils.mention.MentionIteratorFactory;
import it.unipi.di.acube.batframework.datasetPlugins.DatasetBuilder;
import it.unipi.di.acube.batframework.datasetPlugins.GERDAQDataset;

public class TestMentionProcessing {
	
	private static String itToString(GreedyMentionIterator it) {
		String ret = "";
		while (it.hasNext()) {
			MentionCandidate candidate = it.next();
			ret += candidate.getMention()+"["+candidate.getQueryStartPosition()+","
					+candidate.getQueryEndPosition()+"] ";
		}
		return ret.trim();
	}
	
	
	
	private static List<String> getModifications(GERDAQDataset dataset, QueryProcessing instance) {
		List<String> changes  = new ArrayList<>();
		for (String query : dataset.getTextInstanceList()) {
			GreedyMentionIterator it = MentionIteratorFactory.getMentionIteratorForQuery(query,false);
			String oldMentions = itToString(new GreedyMentionIterator(query));
			String newMentions = itToString(it);
			if (!oldMentions.equals(newMentions)) {
				changes.add("Changed "+query+":\n\told: "+oldMentions+"\n\tnew: "+newMentions);
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
		for (String modification : modifications) {
			System.out.println(modification);
		}

	}
}
