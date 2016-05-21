package annotatorstub.main;

import annotatorstub.annotator.smaph.SmaphSAnnotator;
import annotatorstub.annotator.smaph.SmaphSNoPruning;

import java.util.Optional;

/**
 * Main class, for demonstration and debugging for the time being
 * The C2W task is solved without the pruning set followed in order
 * to gather the P/R/F1 statistics for the candidate generation stage
 *
 * @author andreasgeorgiadis
 */

public class SMAPHMain {

    /**
     * Toy example to function as a smoke test for the SMAPH-S feature generation.
     */
    public static void verifySMAPHFeatures() {
        System.out.println("Simple SMAPH-S feature preview.");
        String sampleQuery = "neil armstrong moon landing";
        System.out.printf("Using sample query: %s%n", sampleQuery);

        try {
            SmaphSAnnotator smaphSAnnotator = new SmaphSAnnotator(new SmaphSNoPruning());
            smaphSAnnotator.getCandidatesWithFeatures(sampleQuery);
        } catch (Exception e) {
            System.err.println("Something went wrong running the SMAPH-S annotator.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        verifySMAPHFeatures();

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