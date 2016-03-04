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

import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}
