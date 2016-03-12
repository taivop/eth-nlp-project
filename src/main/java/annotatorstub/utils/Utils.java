/**
 *  Copyright 2014 Marco Cornolti
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package annotatorstub.utils;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unipi.di.acube.batframework.cache.BenchmarkCache;
import it.unipi.di.acube.batframework.data.Annotation;
import it.unipi.di.acube.batframework.data.Tag;
import it.unipi.di.acube.batframework.metrics.Metrics;
import it.unipi.di.acube.batframework.metrics.MetricsResultSet;
import it.unipi.di.acube.batframework.metrics.StrongAnnotationMatch;
import it.unipi.di.acube.batframework.metrics.StrongTagMatch;
import it.unipi.di.acube.batframework.problems.A2WDataset;
import it.unipi.di.acube.batframework.problems.A2WSystem;
import it.unipi.di.acube.batframework.utils.ProblemReduction;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

public class Utils {
	private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	public static final String BASE_DBPEDIA_URI = "http://dbpedia.org/resource/";
	public static final String WIKITITLE_ENDPAR_REGEX = "\\s*\\([^\\)]*\\)\\s*$";

	public static JSONObject httpQueryJson(String urlAddr) {
		String resultStr = null;
		try {
			URL url = new URL(urlAddr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			if (conn.getResponseCode() != 200) {
				Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A");
				LOG.error("Got HTTP error {}. Message is: {}", conn.getResponseCode(), s.next());
				s.close();
			}

			Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
			resultStr = s.hasNext() ? s.next() : "";

			return new JSONObject(resultStr);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void serializeResult(A2WSystem ann, A2WDataset ds, File output) throws FileNotFoundException, IOException {
		List<HashSet<Annotation>> annotations = BenchmarkCache.doA2WAnnotations(ann, ds);
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(output));
		oos.writeObject(annotations);
		oos.close();
	}
	
	public static List<HashSet<Annotation>> deserializeResult(File input) throws ClassNotFoundException, FileNotFoundException, IOException {
		ObjectInputStream oos = new ObjectInputStream(new FileInputStream(input));
		Object o = oos.readObject();
		oos.close();
		return (List<HashSet<Annotation>>) o;
	}
	
	public static void evaluateSerializedResults(A2WDataset ds, File input) throws FileNotFoundException, IOException, ClassNotFoundException {
		List<HashSet<Annotation>> results = deserializeResult(input);
		assert results.size() == ds.getSize();
		
		Metrics<Tag> metricsTag = new Metrics<>();
		MetricsResultSet C2WRes = metricsTag.getResult(ProblemReduction.A2WToC2WList(results), ds.getC2WGoldStandardList(), new StrongTagMatch(WikipediaApiInterface.api()));
		Utils.printMetricsResultSet("C2W", C2WRes, input.getName());

		Metrics<Annotation> metricsAnn = new Metrics<>();
		MetricsResultSet rsA2W = metricsAnn.getResult(results, ds.getA2WGoldStandardList(), new StrongAnnotationMatch(WikipediaApiInterface.api()));
		Utils.printMetricsResultSet("A2W-SAM", rsA2W, input.getName());
	}

	public static void printMetricsResultSet(String exp, MetricsResultSet rs, String annName){
		System.out.format(Locale.ENGLISH, "%s\t%s std-P/R/F1: %.3f/%.3f/%.3f\t%s%n", exp, rs, rs.getPrecisionStdDev(), rs.getRecallStdDev(), rs.getF1StdDev(), annName);
	}
}
