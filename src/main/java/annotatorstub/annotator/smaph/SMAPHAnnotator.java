package annotatorstub.annotator.smaph;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.ConnectException;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import annotatorstub.annotator.FakeAnnotator;
import annotatorstub.utils.bing.BingResult;
import annotatorstub.utils.bing.BingSearchAPI;
import it.unipi.di.acube.batframework.data.Tag;
import it.unipi.di.acube.batframework.utils.AnnotationException;

/**
 * SMAPH annotator class
 * 
 * @author andreasgeorgiadis
 *
 */

public class SMAPHAnnotator extends FakeAnnotator {
	
	private FileOutputStream file;
	
	private int queryCnt = 0;
	
	private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private static final int TOP_K_SNIPPETS = 25;
	
	private static BingSearchAPI bingApi;
	
	private CandidateEntitiesGenerator candidateEntitiesGenerator;
	
	public SMAPHAnnotator(){
		BingSearchAPI.KEY = "crECheFN9wPg0oAJWRZM7nfuJ69ETJhMzxXXjchNMSM";
		bingApi = BingSearchAPI.getInstance();
		
		candidateEntitiesGenerator = new CandidateEntitiesGenerator();
		
		try {
			file = new FileOutputStream("processed_queries.txt");
		}
		catch (FileNotFoundException e){
			logger.warn("Could not create processed queries file");
		}
	}
	
	/**
	 * Fetches candidate entities based on bing search results
	 * 
	 * @param text : search query to be annotated
	 * @return candidate annotations based on bing search (E1, E2, E3 in papers)
	 */
	
	private HashSet<Tag> fetchEntities(String text) throws Exception {
		//System.out.println(text);
		
		BingResult bingResult = bingApi.query(text);
		CandidateEntities candidateEntities = 
				candidateEntitiesGenerator.generateCandidateEntities(bingResult,TOP_K_SNIPPETS,CandidateEntitiesGenerator.QueryMethod.ALL_OVERLAP);
		
		HashSet<Tag> entities = new HashSet<Tag>();
		
		HashSet<Tag> entitiesQuery = (HashSet<Tag>) candidateEntities.getEntitiesQuery().stream().map(s -> new Tag(s)).collect(Collectors.toSet());
		HashSet<Tag> entitiesQueryExtended = (HashSet<Tag>) candidateEntities.getEntitiesQueryExtended().stream().map(s -> new Tag(s)).collect(Collectors.toSet());
		HashSet<Tag> entitiesQuerySnippetsTAGME = (HashSet<Tag>) candidateEntities.getEntitiesQuerySnippetsWAT().stream().map(s -> new Tag(s)).collect(Collectors.toSet());
		
		entities.addAll(entitiesQuery);
		entities.addAll(entitiesQueryExtended);
		entities.addAll(entitiesQuerySnippetsTAGME);
		
		return entities;
	}
	
	/**
	 * Does not solve the C2W problem, since it bypasses the pruning step.
	 * Used only for benchmarking the candidate entity generation process 
	 * for the time being.
	 */
	@Override
	public HashSet<Tag> solveC2W(String text) throws AnnotationException {		
		try {
			file.write(("Query count = " + (queryCnt++) + " Query = " + text + "\n").getBytes());
			return fetchEntities(text);
		}
		catch (ConnectException e){
			logger.warn(e.getMessage());
			return new HashSet<Tag>();
		}
		catch (RuntimeException e){
			if (e.getCause().getCause() instanceof IOException){
				logger.warn(e.getMessage());
				return new HashSet<Tag>();
			}
			else
				throw new AnnotationException(e.getMessage());
		}
		catch (Exception e){
			throw new AnnotationException(e.getMessage());
		}
	}
	
	
	@Override
	public String getName() {
		return "SMAPH annotator";
	}

}
