package annotatorstub.main;

import annotatorstub.annotator.smaph.CandidateEntitiesGenerator;
import annotatorstub.annotator.smaph.Smaph1Pruner;
import annotatorstub.annotator.smaph.Smaph1RemoteSvmPruner;
import annotatorstub.annotator.smaph.SmaphSAnnotator;
import annotatorstub.annotator.wat.HelperWATAnnotator;
import annotatorstub.utils.PythonApiInterface;
import annotatorstub.utils.Utils;
import annotatorstub.utils.WATRelatednessComputer;
import it.unipi.di.acube.batframework.cache.BenchmarkCache;
import it.unipi.di.acube.batframework.data.Annotation;
import it.unipi.di.acube.batframework.data.Tag;
import it.unipi.di.acube.batframework.datasetPlugins.DatasetBuilder;
import it.unipi.di.acube.batframework.metrics.Metrics;
import it.unipi.di.acube.batframework.metrics.MetricsResultSet;
import it.unipi.di.acube.batframework.metrics.StrongAnnotationMatch;
import it.unipi.di.acube.batframework.metrics.StrongTagMatch;
import it.unipi.di.acube.batframework.problems.A2WDataset;
import it.unipi.di.acube.batframework.utils.DumpData;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class BenchmarkMain {
    public static void main(String[] args) throws Exception {
        WikipediaApiInterface wikiApi = WikipediaApiInterface.api();
        A2WDataset ds = DatasetBuilder.getGerdaqTest();
//		FakeAnnotator ann = new FakeAnnotator();
//		BaselineAnnotator ann = new BaselineAnnotator();
//		WATAnnotator ann = new WATAnnotator("wikisense.mkapp.it", 80, "salsa-auth");
        /*
         This part is using the Smaph-1/S annotation system. To get it working, there are a few
         steps which need to be done.
            1) Run SMAPHSFeaturesMain to dump the features in a CSV file.
            2) (Optional) Play around with that CSV data in an iPython notebook and do some
               hyperparameter tuning. Please keep in mind that the data is highly imbalanced
               towards negative samples, so traditional grid search techniques (such as relying
               on cross-validation) don't tend to work very well and have a rather big variance.
            3) Use the 'train_smaph_model.py' program to train an SVM (or any other classifier
               which you think might perform well, wink@Berni, even a NN :), and pickle it to
               some file.
            4) Pass that file to the 'startPythonServer' function below. It will spawn the
               sklearn API server used by the SMAPH pipeline.
            5) The pipeline should now be able to run. Please note that it will be quite slow
               until the 'HelperWATAnnotator' populates its JSON cache and stops hitting the WAT API
               so hard.
            6) (Optional) Move these instructions to a more appropriate place, if applicable.
         */

        try (PythonApiInterface svmApi = new PythonApiInterface(5000)) {
//            svmApi.startPythonServer("models/svc-nonlin-gerdaq-a-b-c-0.7.pkl");
//            svmApi.startPythonServer("models/sgdc-linear-gerdaq-a-b-alpha-0.01-hinge-l1.pkl");
            svmApi.startPythonServer("models/svc-20f-nonlin-gerdaq-a-b-c-0.5.pkl");
            SmaphSAnnotator ann = new SmaphSAnnotator(
                Optional.of(new Smaph1RemoteSvmPruner(svmApi))
//                CandidateEntitiesGenerator.QueryMethod.ALL
            );
//            SmaphSAnnotator ann = new SmaphSAnnotator(Optional.empty());

            WATRelatednessComputer.setCache("relatedness.cache");

            List<HashSet<Tag>> resTag = BenchmarkCache.doC2WTags(ann, ds);
            List<HashSet<Annotation>> resAnn = BenchmarkCache.doA2WAnnotations(ann, ds);
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
            Utils.printMetricsResultSet("C2W", C2WRes, ann.getName());

            Metrics<Annotation> metricsAnn = new Metrics<>();
            MetricsResultSet rsA2W = metricsAnn.getResult(
                resAnn,
                ds.getA2WGoldStandardList(),
                new StrongAnnotationMatch(wikiApi));
            Utils.printMetricsResultSet("A2W-SAM", rsA2W, ann.getName());

            Utils.serializeResult(ann, ds, new File("annotations.bin"));
            wikiApi.flush();
            WATRelatednessComputer.flush();

            // TODO-LOW(andrei): Use more dependency injection instead of this.
            ((HelperWATAnnotator) ann.getAuxiliaryAnnotator()).getRequestCache().flush();
        }
    }

}
