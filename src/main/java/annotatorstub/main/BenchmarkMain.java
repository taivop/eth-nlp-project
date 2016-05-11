package annotatorstub.main;

import annotatorstub.annotator.smaph.Smaph1RemoteSvmPruner;
import annotatorstub.annotator.smaph.SmaphSAnnotator;
import annotatorstub.utils.PythonApiInterface;
import it.unipi.di.acube.batframework.cache.BenchmarkCache;
import it.unipi.di.acube.batframework.data.Annotation;
import it.unipi.di.acube.batframework.data.Tag;
import it.unipi.di.acube.batframework.datasetPlugins.DatasetBuilder;
import it.unipi.di.acube.batframework.metrics.Metrics;
import it.unipi.di.acube.batframework.metrics.MetricsResultSet;
import it.unipi.di.acube.batframework.metrics.StrongAnnotationMatch;
import it.unipi.di.acube.batframework.metrics.StrongTagMatch;
import it.unipi.di.acube.batframework.problems.A2WDataset;
import it.unipi.di.acube.batframework.systemPlugins.WATAnnotator;
import it.unipi.di.acube.batframework.utils.DumpData;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

import java.io.File;
import java.util.HashSet;
import java.util.List;

import annotatorstub.annotator.BaselineAnnotator;
import annotatorstub.annotator.FakeAnnotator;
import annotatorstub.utils.Utils;
import annotatorstub.utils.WATRelatednessComputer;

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
               sklearn API used by the SMAPH pipeline.
            5) The pipeline should now be able to run. Please note that it will be quite slow
               until the 'HelperWATAnnotator' populates its JSON cache and stops hitting the WAT API
               so hard.
            6) (Optional) Move these instructions to a more appropriate place, if applicable.
         */
		try(PythonApiInterface svmApi = new PythonApiInterface(5000)) {
            svmApi.startPythonServer("models/svc-nonlin-vanilla-dupe-with-scaling.pkl");
            SmaphSAnnotator ann = new SmaphSAnnotator(new Smaph1RemoteSvmPruner(svmApi));

            WATRelatednessComputer.setCache("relatedness.cache");

            List<HashSet<Tag>> resTag = BenchmarkCache.doC2WTags(ann, ds);
            List<HashSet<Annotation>> resAnn = BenchmarkCache.doA2WAnnotations(ann, ds);
            DumpData.dumpCompareList(ds.getTextInstanceList(), ds.getA2WGoldStandardList(), resAnn, wikiApi);

            Metrics<Tag> metricsTag = new Metrics<>();
            MetricsResultSet C2WRes = metricsTag.getResult(resTag, ds.getC2WGoldStandardList(), new StrongTagMatch(wikiApi));
            Utils.printMetricsResultSet("C2W", C2WRes, ann.getName());

            Metrics<Annotation> metricsAnn = new Metrics<>();
            MetricsResultSet rsA2W = metricsAnn.getResult(resAnn, ds.getA2WGoldStandardList(), new StrongAnnotationMatch(wikiApi));
            Utils.printMetricsResultSet("A2W-SAM", rsA2W, ann.getName());

            Utils.serializeResult(ann, ds, new File("annotations.bin"));
            wikiApi.flush();
            WATRelatednessComputer.flush();
        }
	}

}
