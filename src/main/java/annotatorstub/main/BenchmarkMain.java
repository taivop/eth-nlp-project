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
		try(PythonApiInterface svmApi = new PythonApiInterface(5000)) {
            svmApi.startPythonServer("models/svc-nonlin-vanilla.pkl");
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
