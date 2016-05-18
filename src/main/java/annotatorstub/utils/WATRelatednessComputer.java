package annotatorstub.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.net.URLEncoder;
import java.io.IOException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unipi.di.acube.batframework.utils.Pair;

public class WATRelatednessComputer implements Serializable {
	private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;
	private static WATRelatednessComputer instance = new WATRelatednessComputer();
	private Object2DoubleOpenHashMap<Pair<Integer,Integer>> cacheJaccard = new Object2DoubleOpenHashMap<>();
	private Object2DoubleOpenHashMap<Pair<Integer,Integer>> cacheMW = new Object2DoubleOpenHashMap<>();
	private Object2DoubleOpenHashMap<Pair<String,Integer>> cacheComm = new Object2DoubleOpenHashMap<>();
	private Object2ObjectOpenHashMap<String, int[]> cacheAnchors = new Object2ObjectOpenHashMap<>();
	private Object2DoubleOpenHashMap<String> cacheLp = new Object2DoubleOpenHashMap<>();
	private static long flushCounter = 0;
	private static final int FLUSH_EVERY = 200;
	private static final String URL_TEMPLATE_JACCARD = "http://wikisense.mkapp.it/rel/id?src=%d&dst=%d&relatedness=jaccard";
	private static final String URL_TEMPLATE_MW = "http://wikisense.mkapp.it/rel/id?src=%d&dst=%d&relatedness=mw";
	private static final String URL_TEMPLATE_SPOT = "http://wikisense.mkapp.it/tag/spot?text=%s";
	private static String resultsCacheFilename = null;
	
	public synchronized void increaseFlushCounter()
			throws FileNotFoundException, IOException {
		flushCounter++;
		if ((flushCounter % FLUSH_EVERY) == 0)
			flush();
	}

	public static synchronized void flush() throws FileNotFoundException,
			IOException {
		if (flushCounter > 0 && resultsCacheFilename != null) {
			LOG.info("Flushing relatedness cache... ");
			new File(resultsCacheFilename).createNewFile();
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(resultsCacheFilename));
			oos.writeObject(instance);
			oos.close();
			LOG.info("Flushing relatedness cache done.");
		}
	}
	
	public static void setCache(String cacheFilename)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		if (resultsCacheFilename != null
				&& resultsCacheFilename.equals(cacheFilename))
			return;
		LOG.info("Loading relatedness cache...");
		resultsCacheFilename = cacheFilename;
		if (new File(resultsCacheFilename).exists()) {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
					resultsCacheFilename));
			instance = (WATRelatednessComputer) ois.readObject();
			ois.close();
		}
	}

	private double queryJsonRel(int wid1, int wid2, String urlTemplate) {
		String url = String.format(urlTemplate, wid1, wid2);
		LOG.info(url);
		JSONObject obj = Utils.httpQueryJson(url);
		try {
			increaseFlushCounter();
			double rel = obj.getDouble("value");
			LOG.debug(" -> " + rel);
			return rel;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private double getGenericRelatedness(int wid1, int wid2, Object2DoubleOpenHashMap<Pair<Integer,Integer>> cache, String url){
		if (wid2 < wid1) {
			int tmp = wid2;
			wid2 = wid1;
			wid2 = tmp;
		}
		Pair <Integer,Integer> p = new Pair<Integer,Integer>(wid1,wid2);
		if (!cache.containsKey(p))
			cache.put(p, queryJsonRel(wid1, wid2, url));
			
		return cache.getDouble(p);
	}
	
	public static double getJaccardRelatedness(int wid1, int wid2) {
		return instance.getGenericRelatedness(wid1, wid2, instance.cacheJaccard, URL_TEMPLATE_JACCARD);
	}

	public static double getMwRelatedness(int wid1, int wid2) {
		return instance.getGenericRelatedness(wid1, wid2, instance.cacheMW, URL_TEMPLATE_MW);
	}

	public static double getLp(String anchor) {
		if (!instance.cacheLp.containsKey(anchor))
			instance.cacheLp.put(anchor, queryJsonLp(anchor));
		return instance.cacheLp.get(anchor);
	}

	public static double getCommonness(String anchor, int wid) {
		if (!instance.cacheAnchors.containsKey(anchor))
			queryJsonComm(anchor);

		Pair<String, Integer> key = new Pair<>(anchor, wid);
		if (!instance.cacheComm.containsKey(key))
			return 0.0;
		return instance.cacheComm.get(key);
	}

	public static int[] getLinks(String anchor) {
		if (!instance.cacheAnchors.containsKey(anchor))
			queryJsonComm(anchor);
		return instance.cacheAnchors.get(anchor);
	}

	private static void queryJsonComm(String anchor) {
		instance.cacheAnchors.put(anchor, new int[]{});
		try {
			String url = String.format(URL_TEMPLATE_SPOT, URLEncoder.encode(anchor, "utf-8"));
			LOG.debug("Querying {}", url);
			JSONObject obj = Utils.httpQueryJson(url);
			instance.increaseFlushCounter();
			JSONArray spots = obj.getJSONArray("spots");
			for (int i = 0; i < spots.length(); i++) {
				JSONObject objI = spots.getJSONObject(i);
				JSONArray ranking = objI.getJSONArray("ranking");
				String anchorC = objI.getString("spot");
				int[] candidates = new int[ranking.length()];
				instance.cacheAnchors.put(anchorC, candidates);
				for (int j = 0; j < ranking.length(); j++) {
					JSONObject candidate = ranking.getJSONObject(j);
					int widC = candidate.getInt("id");
					candidates[j] = widC;
					double commonnessC = candidate.getDouble("commonness");
					instance.cacheComm.put(new Pair<String, Integer>(anchorC, widC), commonnessC);
				}
			}
		} catch (Exception e) {
			if ((e.getCause() != null) && (e.getCause() instanceof IOException)){
				return;
			}
			else
				throw new RuntimeException(e);
		}
	}

	private static double queryJsonLp(String anchor) {
		try {
			String url = String.format(URL_TEMPLATE_SPOT, URLEncoder.encode(anchor, "utf-8"));
			LOG.debug("Querying {}", url);
			JSONObject obj = Utils.httpQueryJson(url);
			instance.increaseFlushCounter();
			JSONArray spots = obj.getJSONArray("spots");
			for (int i = 0; i < spots.length(); i++) {
				JSONObject objI = spots.getJSONObject(i);
				if (objI.getString("spot").equals(anchor)){
					double lp = objI.getDouble("linkProb");
					return lp;
				}
			}
			return 0.0;
		} catch (Exception e) {
			if ((e.getCause() != null) && (e.getCause() instanceof IOException)){
				return 0.0;
			}
			else
				throw new RuntimeException(e);
		}
	}

}
