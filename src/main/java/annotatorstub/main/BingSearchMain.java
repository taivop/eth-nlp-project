package annotatorstub.main;

import org.codehaus.jettison.json.JSONObject;

import it.unipi.di.acube.BingInterface;

public class BingSearchMain {
	public static void main(String[] args) throws Exception {
		BingInterface bing = new BingInterface("<your key goes here>");
		JSONObject a = bing.queryBing("funny kittens");

		// see: http://datamarket.azure.com/dataset/bing/search#schema for
		// query/response format
		System.out.println(a.getJSONObject("d").getJSONArray("results").getJSONObject(0).toString(4));
	}
}
