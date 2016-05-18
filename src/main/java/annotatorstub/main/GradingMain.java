package annotatorstub.main;

import annotatorstub.annotator.smaph.CandidateEntitiesGenerator;
import annotatorstub.annotator.smaph.SmaphSAnnotator;
import annotatorstub.annotator.smaph.SmaphSRemoteSvmPruner;
import annotatorstub.annotator.wat.HelperWATAnnotator;
import annotatorstub.utils.PythonApiInterface;
import annotatorstub.utils.Utils;
import annotatorstub.utils.WATRelatednessComputer;
import annotatorstub.utils.caching.WATRequestCache;
import it.unipi.di.acube.batframework.cache.BenchmarkCache;
import it.unipi.di.acube.batframework.data.Annotation;
import it.unipi.di.acube.batframework.data.Mention;
import it.unipi.di.acube.batframework.data.Tag;
import it.unipi.di.acube.batframework.datasetPlugins.YahooWebscopeL24Dataset;
import it.unipi.di.acube.batframework.metrics.Metrics;
import it.unipi.di.acube.batframework.metrics.MetricsResultSet;
import it.unipi.di.acube.batframework.metrics.StrongAnnotationMatch;
import it.unipi.di.acube.batframework.metrics.StrongTagMatch;
import it.unipi.di.acube.batframework.problems.A2WDataset;
import it.unipi.di.acube.batframework.utils.DumpData;
import it.unipi.di.acube.batframework.utils.ProblemReduction;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class GradingMain {
	
	private static int totalQueries;

    public static A2WDataset goldStandardYahooSubset(
        String yahooFullFile,
        String gradingDataFile
    ) throws SAXException {
        try {
            A2WDataset unannotated = new YahooWebscopeL24Dataset(gradingDataFile);
            A2WDataset fullyAnnotated = new YahooWebscopeL24Dataset(yahooFullFile);
            
            // Keep a dataset with annotated queries which only occur in the grading set.
            Set<String> gradedQuerySet = new HashSet<>(unannotated.getTextInstanceList());
            
            totalQueries = gradedQuerySet.size();
            
            System.out.println("Unannotated query count: " + unannotated.getTextInstanceList().size());
            System.out.println("Unique unannotated query count: " + gradedQuerySet.size());
            System.out.println("A discrepancy is normal because of duplicate common queries such " +
                    "as 'facebook or 'twilight green day'.");

            // These are the components of our newly-generated dataset.
            List<String> gradedQueryList = new ArrayList<>();
            List<HashSet<Annotation>> subsampledGold = new ArrayList<>();

            // Add only those annotated queries from the ground truth which are in the subset
            // provided by the instructors.
            for (int i = 0; i < fullyAnnotated.getA2WGoldStandardList().size(); i++) {
                if (gradedQuerySet.contains(fullyAnnotated.getTextInstanceList().get(i))) {
                    subsampledGold.add(fullyAnnotated.getA2WGoldStandardList().get(i));
                    gradedQueryList.add(fullyAnnotated.getTextInstanceList().get(i));
                }
            }

            System.out.println("Final dataset size: " + gradedQueryList.size());

            A2WDataset out = new A2WDataset() {
                @Override
                public List<HashSet<Annotation>> getA2WGoldStandardList() {
                    return subsampledGold;
                }

                @Override
                public int getTagsCount() {
                    int c = 0;
                    for (HashSet<Annotation> s : subsampledGold) {
                        c += s.size();
                    }
                    return c;
                }

                @Override
                public List<HashSet<Tag>> getC2WGoldStandardList() {
                    return ProblemReduction.A2WToC2WList(subsampledGold);
                }

                @Override
                public List<HashSet<Mention>> getMentionsInstanceList() {
                    return ProblemReduction.A2WToD2WMentionsInstance(subsampledGold);
                }

                @Override
                public List<HashSet<Annotation>> getD2WGoldStandardList() {
                    return subsampledGold;
                }

                @Override
                public int getSize() {
                    return gradedQuerySet.size();
                }

                @Override
                public String getName() {
                    return "Yahoo WebScope Annotated Subset";
                }

                @Override
                public List<String> getTextInstanceList() {
                    return gradedQueryList;
                }
            };

            return out;
        }
        catch (IOException | ParserConfigurationException | XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads the to-be-evaluated dataset for grading.
     *
     * @param useGoldStandard If set, pre-annotates the grading dataset with data from the Yahoo!
     *                        WebScope dataset ground truth. Note that the grading dataset is
     *                        just a subset of the full Yahoo! one. Please don't use the
     *                        resulting numbers (if this is true) for model tuning, as that will
     *                        lead to overfitting.
     */
    private static A2WDataset loadDataset(
        String yahooFullFile,
        String gradingDataFile,
        boolean useGoldStandard
    ) {
        try {
            if (useGoldStandard) {
                // Also load the gold standard for the given queries, so that we may compute the
                // actual F1 (and other) metrics.
                return goldStandardYahooSubset(yahooFullFile, gradingDataFile);
            }
            else {
                // Only load the "blind" grading dataset, which does not contain any gold
                // standard on which to evaluate our pipeline.
                return new YahooWebscopeL24Dataset(gradingDataFile);
            }
        }
        catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            throw new RuntimeException("Could not load grading dataset.", e);
        }
    }

    public static void main(String[] args) {
        WikipediaApiInterface wikiApi = WikipediaApiInterface.api();

        // This is the dataset distributed for grading by the instructors.
        String gradingDataFile = "data/out-domain-dataset-new.xml";

        // This is the fully annotated dataset.
        String yahooFullFile = "data/yahoo-webscope/ydata-search-query-log-to-entities-v1_0.xml";

        boolean useGoldStandard = true;
        A2WDataset ds = loadDataset(yahooFullFile, gradingDataFile, useGoldStandard);

        try (PythonApiInterface svmApi = new PythonApiInterface(5000)) {
//            String modelPickle = "models/m-no-yahoo-lr-c-0.00025.pkl";
            String modelPickle = "models/ada_boost_est_100_tree_depth_3.pkl";
            svmApi.startPythonServer(modelPickle);

            // Use a separate cache when running the benchmark as opposed to when doing the data
            // generation, since this lets us keep the benchmark-only cache small. The data gen
            // one, especially when also using the Yahoo! data, ends up blowing up to several Gb,
            // and takes around a minute to load.
            WATRequestCache watRequestCache = new WATRequestCache(
                "watapi.yahoosubset.cache",
                "Eval cache for subset of Yahoo! queries used for final evaluation.",
                500);

            // Disabling this seems to lead to slightly better overall F1 scores.
            boolean splitMentionsByLP = false;
            SmaphSAnnotator ann = new SmaphSAnnotator(
//                new SmaphSIndividualPruner(new Smaph1RemoteSvmPruner(svmApi)),
                new SmaphSRemoteSvmPruner(svmApi),
                CandidateEntitiesGenerator.QueryMethod.ALL_OVERLAP,
                // look only at the top k = <below> snippets
                25,
                splitMentionsByLP,
                watRequestCache,
                totalQueries);

            WATRelatednessComputer.setCache("relatedness.cache");

            System.out.println("\nDoing C2W tags:\n");
            List<HashSet<Tag>> resTag = BenchmarkCache.doC2WTags(ann, ds);
            System.out.println("\nDoing D2W annotations:\n");
            List<HashSet<Annotation>> resAnn = BenchmarkCache.doA2WAnnotations(ann, ds);
            if(useGoldStandard) {
                DumpData.dumpCompareList(
                    ds.getTextInstanceList(),
                    ds.getA2WGoldStandardList(),
                    resAnn,
                    wikiApi);

                Metrics<Tag> metricsTag = new Metrics<>();
                MetricsResultSet C2WRes = metricsTag.getResult(
                    resTag,
                    ds.getC2WGoldStandardList(),
                    new StrongTagMatch(wikiApi));
                System.out.println("C2W results:");
                Utils.printMetricsResultSet("C2W", C2WRes, ann.getName());

                Metrics<Annotation> metricsAnn = new Metrics<>();
                MetricsResultSet rsA2W = metricsAnn.getResult(
                    resAnn,
                    ds.getA2WGoldStandardList(),
                    new StrongAnnotationMatch(wikiApi));
                System.out.println("A2W-SAM results:");
                Utils.printMetricsResultSet("A2W-SAM", rsA2W, ann.getName());
            }
            else {
                System.out.println("No ground truth available, so no metrics to print.");
            }

            Utils.serializeResult(
                ann,
                ds,
                new File("annotation-barsan-kratzwald-georgiadis-pungas.bin"));
            wikiApi.flush();
            WATRelatednessComputer.flush();

            ((HelperWATAnnotator) ann.getAuxiliaryAnnotator()).getRequestCache().flush();
            System.out.println("Was using following model: " + modelPickle);
        }
        catch(IOException e) {
            throw new RuntimeException("IOException occurred during annotation. Aborting process" +
                ".", e);
        }
        catch(Exception e) {
            throw new RuntimeException("Critical exception occurred during annotation. Aborting " +
                "process.", e);
        }
    }
}
