package annotatorstub.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class PythonApiInterface implements Closeable {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final String API_ADDRESS    = "http://localhost";
    private final String API_ENDPOINT   = "predict";
    private final String SEPARATOR      = ",";
    private Integer API_PORT;
    private Process serverProcess;

    public PythonApiInterface(Integer port) {
        API_PORT = port;
    }

    /**
     * Start the Python server for serving predictions over HTTP.
     */
    public void startPythonServer(String modelPickleFile) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "python",
                "src/main/python/server.py",
                String.valueOf(API_PORT),
                SEPARATOR,
                modelPickleFile);

        File logFolder = new File("log");
        if (!logFolder.exists() && !logFolder.mkdir()) {
            throw new RuntimeException("Could not create log directory for Python.");
        }

        String pyStdOutFileName = "log/pyout.txt";
        String pyStdErrFileName = "log/pyerr.txt";
        logger.info("Starting Python server: {}", processBuilder.command());
        logger.info("Server stdout will go to: {}", pyStdOutFileName);
        logger.info("Server stderr will go to: {}", pyStdErrFileName);
        File pyStdOut = new File(pyStdOutFileName);
        File pyStdErr = new File(pyStdErrFileName);
        serverProcess = processBuilder
            .redirectOutput(ProcessBuilder.Redirect.to(pyStdOut))
            .redirectError(ProcessBuilder.Redirect.to(pyStdErr))
            .start();

        // TODO hacky way to make sure the server is started...
        TimeUnit.MILLISECONDS.sleep(2000);
    }

    /**
     * Stop the Python server.
     */
    public void stopPythonServer() {
        if(null != serverProcess) {
            serverProcess.destroy();
        }
    }

    private String makeSeparatedString(List<Double> features) {
        String separatedString = "";
        Integer index = 0;
        for(Double f : features) {
            separatedString += f.toString();
            if(index < features.size()-1) {
                separatedString += SEPARATOR;
            }
            index++;
        }

        return separatedString;
    }

    public boolean binClassifyFlask(List<Double> features) throws IOException {
        return binClassifyFlask(features, 3);
    }

    /**
     * Classify given list of features by calling the Python API.
     *
     * TODO(andrei): Reuse same connection.
     *
     * @see http://stackoverflow.com/questions/1359689/how-to-send-http-request-in-java
     */
    public boolean binClassifyFlask(List<Double> features, int retriesLeft) throws IOException {
        if(retriesLeft == 0) {
            throw new RuntimeException("No more Flask API retries left. There's probably " +
                "something wrong with the Python server.");
        }

        String urlParameters = "?features_string=" + makeSeparatedString(features);
        String queryUrl = API_ADDRESS + ":" + API_PORT + "/" + API_ENDPOINT;

        try {
            // Set up connection and send the request
            URL url = new URL(queryUrl + urlParameters);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set some sensible timeouts to prevent us waiting too much in case an error occurs.
            connection.setConnectTimeout(1500);
            connection.setReadTimeout(3000);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.setDoOutput(true);

            // TODO(andrei): Sometimes the Python API seems to act up. Consider implementing retries.
            // Read response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();

            String responseString = response.toString().trim();
//        logger.info(String.format("Response from Python server: %s", responseString));

            // Note: `Boolean.parseBoolean(responseString);` expects 'true'/'false' strings.
            if (responseString.equals("0")) {
                return false;
            }
            else if (responseString.equals("1")) {
                return true;
            }
            else {
                throw new RuntimeException(
                    String.format(
                        "Could not parse classifier output [%s] as a 0/1 boolean.",
                        responseString));
            }
        }
        catch(SocketTimeoutException timeout) {
            System.err.println("Possible issue with Python API. Retrying connection.");
            return binClassifyFlask(features, retriesLeft - 1);
        }
    }

    @Override
    public void close() throws IOException {
        stopPythonServer();
    }
}