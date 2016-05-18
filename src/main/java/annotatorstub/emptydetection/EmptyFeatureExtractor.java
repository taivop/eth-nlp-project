package annotatorstub.emptydetection;

import it.unipi.di.acube.batframework.data.Annotation;
import it.unipi.di.acube.batframework.datasetPlugins.DatasetBuilder;
import it.unipi.di.acube.batframework.datasetPlugins.GERDAQDataset;
import it.unipi.di.acube.batframework.systemPlugins.WATAnnotator;
import it.unipi.di.acube.batframework.utils.AnnotationException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import annotatorstub.annotator.smaph.SmaphSAnnotator;
import annotatorstub.utils.WATRelatednessComputer;
import annotatorstub.utils.bing.BingResult;
import annotatorstub.utils.bing.BingSearchAPI;
import annotatorstub.utils.mention.GreedyMentionIterator;
import annotatorstub.utils.mention.MentionCandidate;
import annotatorstub.utils.mention.SmaphCandidate;

public class EmptyFeatureExtractor {
    private static BingSearchAPI bingApi;
	private static WATAnnotator ann = new WATAnnotator("wikisense.mkapp.it", 80, "salsa-auth");
	private static SmaphSAnnotator smaphSAnnotator = new SmaphSAnnotator(Optional.empty());

	private static String getFeatures(String query) {
		BingResult bingResult = null;
        
		try {
            bingResult = bingApi.query(query);
        } catch (Exception e) {
            throw new AnnotationException(e.getMessage()); 
        }
		
		//was query changed? bingResult.getAlteredQueryString().equals(query);
		long f1_total_results = bingResult.getWebTotal();
		long f2_wikipeida_results = bingResult.getWikipediaResults().size();
		long f3_wikipedia_results_extended = bingResult.getExtendedWikipediaResults().size();
		
		GreedyMentionIterator it = new GreedyMentionIterator(bingResult.getAlteredQueryString());
		double lp = 0;
		double count = 0; 
		while (it.hasNext()) {
			MentionCandidate candidate = it.next();
			if (candidate.getMention().length()>1) {
				lp += WATRelatednessComputer.getLp(candidate.getMention());
				count += 1.0;
			}
		}
		
        List<SmaphCandidate> candidate = smaphSAnnotator.getCandidatesWithFeatures(query);
        System.out.println(candidate.get(0).getFeatures().size());
        System.exit(-1);
		
		double f4_average_lp = (lp/count);
		int f5_number_wat = ann.solveA2W(query).size();
		return f1_total_results+","+f2_wikipeida_results+","+f4_average_lp+","+f5_number_wat+","+query.length();
	}
	
	
	private static void writeDataset(GERDAQDataset geradqDataset, BufferedWriter bw) throws IOException {
		List<HashSet<Annotation>> goldStandard = geradqDataset.getA2WGoldStandardList();
		List<String> queries = geradqDataset.getTextInstanceList();
		
		for (int i = 0; i < queries.size(); i++) {
			boolean label = false;
			if (goldStandard.get(i).size()==0) {
				label = true;
			}
			if (label) {
				bw.write(1+","+getFeatures(queries.get(i)));
				bw.newLine();
			} else {
				bw.write(0+","+getFeatures(queries.get(i)));
				bw.newLine();
			}
		}

	}
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
        BingSearchAPI.KEY = "crECheFN9wPg0oAJWRZM7nfuJ69ETJhMzxXXjchNMSM";
        bingApi = BingSearchAPI.getInstance();
        WATRelatednessComputer.setCache("relatedness.cache");
		
        File fout = new File("./data/nonempty_train.csv");
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        
		
		writeDataset(DatasetBuilder.getGerdaqTrainA(), bw);
		writeDataset(DatasetBuilder.getGerdaqTrainB(), bw);
		
		bw.close();

        fout = new File("./data/nonempty_valid.csv");
		fos = new FileOutputStream(fout);
		bw = new BufferedWriter(new OutputStreamWriter(fos));
		
		
		writeDataset(DatasetBuilder.getGerdaqDevel(), bw);


		bw.close();

	}
}
