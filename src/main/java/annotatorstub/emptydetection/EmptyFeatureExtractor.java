package annotatorstub.emptydetection;

import it.unipi.di.acube.batframework.data.Annotation;
import it.unipi.di.acube.batframework.datasetPlugins.DatasetBuilder;
import it.unipi.di.acube.batframework.datasetPlugins.GERDAQDataset;
import it.unipi.di.acube.batframework.systemPlugins.WATAnnotator;
import it.unipi.di.acube.batframework.utils.AnnotationException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
		System.err.println(query);
		System.err.println(query);
		System.err.println(query);
        
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
		
		double f4_average_lp = (lp/count);
		int f5_number_wat = ann.solveA2W(query).size();
		int f6_query_lenght = query.length();
		
		List<SmaphCandidate> candidateList = smaphSAnnotator.getCandidatesWithFeatures(query);
		int numFeatures = 24;
		List<Double> smaphSfeatures = new ArrayList<Double>();
		for (int i=0;i<numFeatures;i++) {
			smaphSfeatures.add(i, 0.0);
		}
		
        for (SmaphCandidate candidate : candidateList) {
    		for (int i=0;i<numFeatures;i++) {
    			smaphSfeatures.set(i, smaphSfeatures.get(i)+candidate.getFeatures().get(i));
    		}
        }
       
        String smaphSfeatureString = "";
		for (int i=0;i<numFeatures;i++) {
			smaphSfeatureString += ","+(smaphSfeatures.get(i)/candidateList.size());
		}

		return f1_total_results+","+f2_wikipeida_results+","+f3_wikipedia_results_extended+","+f4_average_lp+","+f5_number_wat+","+f6_query_lenght
				+smaphSfeatureString;
	}
	
	private static String getAlteredQueryString(String query) {
		BingResult bingResult = null;
        
		try {
            bingResult = bingApi.query(query);
        } catch (Exception e) {
            throw new AnnotationException(e.getMessage()); 
        }
		
		return bingResult.getAlteredQueryString().replace(',', ' ');
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
				bw.write("1,"+queries.get(i)+","+getFeatures(queries.get(i)));
				bw.newLine();
			} else {
				bw.write("0,"+queries.get(i)+","+getFeatures(queries.get(i)));
				bw.newLine();
			}
		}

	}
	
	private static void writeAdaptedDataset(String csvFileName, BufferedWriter bw) throws IOException {
		String line;
		try (
		    InputStream fis = new FileInputStream(csvFileName);
		    InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
		    BufferedReader br = new BufferedReader(isr);
		) {
			//TODO REMOVE!!!
		    while ((line = br.readLine()) != null) {
		        String[] label_string = line.split(",");
		        String query_text = label_string.length==1 ? "" : label_string[1]; 
		        query_text = query_text.equals("-") ? "sfd" : label_string[1];  
		        bw.write(label_string[0]+","+getFeatures(query_text));
				bw.newLine();
		    }
		}

	}
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
        BingSearchAPI.KEY = "+MkADwXpOGeryP7sNqlkbtUeZYhs8mUeUBsNq++Yk1U";
        bingApi = BingSearchAPI.getInstance();
        WATRelatednessComputer.setCache("relatedness.cache");
		/*
        File fout = new File("./data/nonempty_lululu_dict_strict.csv");
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
		writeAdaptedDataset("./data/altered_queries_lululu_dict_strict.csv",bw);
		bw.close();
		
		
		fout = new File("./data/nonempty_valid_dict.csv");
		fos = new FileOutputStream(fout);
		bw = new BufferedWriter(new OutputStreamWriter(fos));
		writeAdaptedDataset("./data/altered_queries_valid_dict.csv",bw);
		bw.close();
		
        
        File fout = new File("./data/feature_average_trainAB.csv");
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter  = new BufferedWriter(new OutputStreamWriter(fos));
		
		writeDataset(DatasetBuilder.getGerdaqTrainA(), bw);
		writeDataset(DatasetBuilder.getGerdaqTrainB(), bw);

		bw.close();
		*/
		File fout = new File("./data/feature_average_devel.csv");
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
		
		writeDataset(DatasetBuilder.getGerdaqDevel(), bw);

		bw.close();

        fout = new File("./data/feature_average_test.csv");
        fos = new FileOutputStream(fout);
        bw = new BufferedWriter(new OutputStreamWriter(fos));
		
		writeDataset(DatasetBuilder.getGerdaqTest(), bw);

		bw.close();


	}
}

