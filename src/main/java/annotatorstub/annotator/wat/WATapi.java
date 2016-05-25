package annotatorstub.annotator.wat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unipi.di.acube.batframework.data.ScoredAnnotation;
import it.unipi.di.acube.batframework.utils.AnnotationException;

public class WATapi {
	
	private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private static final String WAT_IP = "wikisense.mkapp.it";
	private static final int WAT_PORT = 80;
	private static final int RETRY_N = 50;
	
	private static HashMap<String, byte[]> url2jsonCache = new HashMap<>();
	private static long flushCounter = 0;
	private static final int FLUSH_EVERY = 200;
	private static String resultsCacheFilename = null;
	
	private String method = "base";
	private String windowSize = "";
	private String minCommonness = "0.0";
	private String epsilon = "0.0";
	private String sortedBy = "PAGERANK";
	private String relatedness = "jaccard";
	
	public WATapi(){
		
	}
	
	public WATapi(String method, String windowSize,
			      String minCommonness, String epsilon,
			      String sortedBy, String relatedness){
		this.method = method;
		this.windowSize = windowSize;
		this.minCommonness = minCommonness;
		this.epsilon = epsilon;
		this.sortedBy = sortedBy;
		this.relatedness = relatedness;
	}
	
	public HashSet<ScoredAnnotation> queryWATapi(String text){
		HashSet<ScoredAnnotation> res = new HashSet<ScoredAnnotation>();
		JSONObject obj = null;
		String getParameters = String.format("lang=%s", "en");
		if (!method.equals(""))
			getParameters += String.format("&method=%s", method);
		if (!windowSize.equals(""))
			getParameters += String.format("&windowSize=%s", windowSize);
		if (!epsilon.equals(""))
			getParameters += String.format("&epsilon=%s", epsilon);
		if (!minCommonness.equals(""))
			getParameters += String.format("&minCommonness=%s", minCommonness);
		if (!sortedBy.equals(""))
			getParameters += String.format("&sortedBy=%s", sortedBy);
		if (!relatedness.equals(""))
			getParameters += String.format("&relatedness=%s", relatedness);
		try {
			String url = String.format("http://%s:%d/tag/tag", WAT_IP, WAT_PORT);
			obj = queryJson(text, url, getParameters, RETRY_N);

		} catch (Exception e) {
			logger.error("Got error while querying WikiSense API with GET parameters: "
							+ getParameters + " with text: " + text);
			throw new AnnotationException(
					"An error occurred while querying WikiSense API. Message: "
							+ e.getMessage());
		}

		try {
			JSONArray jsAnnotations = obj.getJSONArray("annotations");
			for (int i = 0; i < jsAnnotations.length(); i++) {
				JSONObject js_ann = jsAnnotations.getJSONObject(i);
				int start = js_ann.getInt("start");
				int end = js_ann.getInt("end");
				int id = js_ann.getInt("id");
				double rho = js_ann.getDouble("rho");
				res.add(new ScoredAnnotation(start, end - start, id,
						(float) rho));
			}
		} catch (JSONException e) {
			e.printStackTrace();
			throw new AnnotationException(e.getMessage());
		}
		return res;
	}
	
	private static JSONObject queryJson(String text, String url, 
			String getParameters, int retry) throws Exception {

		JSONObject parameters = new JSONObject();
		
		parameters.put("text", text);
		
		logger.info("GET Parameters : " + getParameters);
		logger.info("JSON Query : " + parameters.toString());

		String resultStr = null;
		try {
			URL wikiSenseApi = new URL(String.format("%s?%s", url,
					getParameters));

			String cacheKey = wikiSenseApi.toExternalForm()
					+ parameters.toString();
			byte[] compressed = url2jsonCache.get(cacheKey);
			if (compressed != null)
				return new JSONObject(decompress(compressed));

			HttpURLConnection slConnection = (HttpURLConnection) wikiSenseApi
					.openConnection();
			slConnection.setReadTimeout(0);
			slConnection.setDoOutput(true);
			slConnection.setDoInput(true);
			slConnection.setRequestMethod("POST");
			slConnection.setRequestProperty("Content-Type", "application/json");
			slConnection.setRequestProperty("Content-Length", ""
					+ parameters.toString().getBytes().length);

			slConnection.setUseCaches(false);

			DataOutputStream wr = new DataOutputStream(
					slConnection.getOutputStream());
			wr.write(parameters.toString().getBytes());
			wr.flush();
			wr.close();

			if (slConnection.getResponseCode() != 200) {
				Scanner s = new Scanner(slConnection.getErrorStream())
						.useDelimiter("\\A");
				System.err.printf("Got HTTP error %d. Message is: %s%n",
						slConnection.getResponseCode(), s.next());
				s.close();
			}

			Scanner s = new Scanner(slConnection.getInputStream())
					.useDelimiter("\\A");
			resultStr = s.hasNext() ? s.next() : "";

			JSONObject obj = new JSONObject(resultStr);
			url2jsonCache.put(cacheKey, compress(obj.toString()));
			increaseFlushCounter();

			return obj;

		} catch (Exception e) {
			e.printStackTrace();
			try {
				Thread.sleep(3000);
				if (retry > 0)
					return queryJson(text, url, getParameters,
							retry - 1);
				else
					throw e;
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				throw new RuntimeException(e1);
			}
		}
	}
	
	public static synchronized void increaseFlushCounter()
			throws FileNotFoundException, IOException {
		flushCounter++;
		if ((flushCounter % FLUSH_EVERY) == 0)
			flush();
	}

	public static synchronized void flush() throws FileNotFoundException,
			IOException {
		if (flushCounter > 0 && resultsCacheFilename != null) {
			logger.info("Flushing WikiSense cache... ");
			new File(resultsCacheFilename).createNewFile();
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(resultsCacheFilename));
			oos.writeObject(url2jsonCache);
			oos.close();
			logger.info("Flushing WikiSense cache Done.");
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void setCache(String cacheFilename)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		if (resultsCacheFilename != null
				&& resultsCacheFilename.equals(cacheFilename))
			return;
		logger.info("Loading wikisense cache...");
		resultsCacheFilename = cacheFilename;
		if (new File(resultsCacheFilename).exists()) {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
					resultsCacheFilename));
			url2jsonCache = (HashMap<String, byte[]>) ois.readObject();
			ois.close();
		}
	}
	
	public static void unSetCache() {
		url2jsonCache = new HashMap<>();
		System.gc();
	}
	
	/**
	 * Compress a string with GZip.
	 * 
	 * @param str
	 *            the string.
	 * @return the compressed string.
	 * @throws IOException
	 *             if something went wrong during compression.
	 */
	public static byte[] compress(String str) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream gzip = new GZIPOutputStream(out);
		gzip.write(str.getBytes());
		gzip.close();
		return out.toByteArray();
	}

	/**
	 * Decompress a GZipped string.
	 * 
	 * @param compressed
	 *            the sequence of bytes
	 * @return the decompressed string.
	 * @throws IOException
	 *             if something went wrong during decompression.
	 */
	public static String decompress(byte[] compressed) throws IOException {
		GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(
				compressed));
		BufferedReader bf = new BufferedReader(new InputStreamReader(gis));
		String outStr = "";
		String line;
		while ((line = bf.readLine()) != null)
			outStr += line;
		return outStr;
	}

}
