package annotatorstub.main;

import java.util.HashSet;
import java.util.List;

//import annotatorstub.annotator.smaph.SMAPHAnnotator;
import annotatorstub.utils.Utils;
import annotatorstub.utils.WATRelatednessComputer;
import it.unipi.di.acube.batframework.cache.BenchmarkCache;
import it.unipi.di.acube.batframework.data.Tag;
import it.unipi.di.acube.batframework.datasetPlugins.DatasetBuilder;
import it.unipi.di.acube.batframework.metrics.Metrics;
import it.unipi.di.acube.batframework.metrics.MetricsResultSet;
import it.unipi.di.acube.batframework.metrics.StrongTagMatch;
import it.unipi.di.acube.batframework.problems.A2WDataset;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

/**
 * Main class, for demonstration and debugging for the time being
 * The C2W task is solved without the pruning set followed in order
 * to gather the P/R/F1 statistics for the candidate generation stage
 * 
 * @author andreasgeorgiadis
 *
 */

public class SMAPHMain {
		
	public static void main(String[] args) throws Exception {
		/*WikipediaApiInterface wikiApi = WikipediaApiInterface.api();
		A2WDataset dataSet = DatasetBuilder.getGerdaqTrainB();
		SMAPHAnnotator smaphAnnotator = new SMAPHAnnotator();
	
		WATRelatednessComputer.setCache("relatedness.cache");
		
		List<HashSet<Tag>> resTag = BenchmarkCache.doC2WTags(smaphAnnotator, dataSet);
		
		Metrics<Tag> metricsTag = new Metrics<>();
		MetricsResultSet C2WRes = metricsTag.getResult(resTag, dataSet.getC2WGoldStandardList(), new StrongTagMatch(wikiApi));
		Utils.printMetricsResultSet("C2W", C2WRes, smaphAnnotator.getName());
		
		wikiApi.flush();
		WATRelatednessComputer.flush();*/
	}

}
