package annotatorstub.annotator.wat;

import java.util.HashSet;
import java.util.Iterator;

import it.unipi.di.acube.batframework.data.ScoredAnnotation;
import it.unipi.di.acube.batframework.systemPlugins.WATAnnotator;

public class WATapiTester {

	public static void main(String[] args){
		
		String WAT_IP = "wikisense.mkapp.it";
		int WAT_PORT = 80;
		
		String watMethod = "base";
		String watEpsilon = "0.0";
		String watSortBy = "PAGERANK";
		String watRelatedness = "jaccard";
		String watMinLinkProbability = "0.0";
		
		try {
			WATapi.setCache("url2json.cache");
		}
		catch (Exception e){
			System.out.println("cache not set");
		}
		WATAnnotator watAnnotator = new WATAnnotator(WAT_IP,WAT_PORT,watMethod,watSortBy,
				watRelatedness,watEpsilon,watMinLinkProbability);
		
		String query = "Mercedes Benz Cleveland Mercedes Benz Ohio";
		HashSet<ScoredAnnotation> scoredTags = watAnnotator.solveSa2W(query);
		
		Iterator<ScoredAnnotation> iter = scoredTags.iterator();
		while (iter.hasNext()){
			ScoredAnnotation scoredAnnotation = iter.next();
			System.out.println("Start : " + scoredAnnotation.getPosition() + " " + 
							   "End : " + (scoredAnnotation.getPosition() + scoredAnnotation.getLength()) + " " +
							   "Entity : " + scoredAnnotation.getConcept() + " " +
							   "Score : " + scoredAnnotation.getScore());
		}
	}
	
}
