package annotatorstub.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.ConnectException;
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

    private String makeSeparatedFeatureString(List<Double> features) {
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

    private String makeProbabilisticFlagString(boolean isProbabilistic) {
        String flagString = "probabilistic=";
        flagString += isProbabilistic ? "1" : "0";
        return flagString;
    }

    public boolean binClassifyFlask(List<Double> features) throws IOException {
        Double prediction = binClassifyFlask(features, false, 3);
        if(prediction == 0.0) {
            return false;
        } else if(prediction == 1.0) {
            return true;
        } else {
            throw new RuntimeException(
                    String.format("Could not parse classifier output [%.3f] as a 0/1 boolean.", prediction)
            );
        }
    }

    //TODO (Taivo) I know the method name sucks but backwards-compatibility, coherent naming and gangsta rap and made me do it
    public Double binClassifyFlaskProbabilistic(List<Double> features) throws IOException {
        return binClassifyFlask(features, true, 3);
    }

    /**
     * Classify given list of features by calling the Python API.
     *
     * @see
     *  <a href="http://stackoverflow.com/questions/1359689/how-to-send-http-request-in-java">
     *    http://stackoverflow.com/questions/1359689/how-to-send-http-request-in-java
     *  </a>
     */
    private Double binClassifyFlask(
        List<Double> features,
        boolean probabilistic,
        int retriesLeft) throws IOException {
        if(retriesLeft == 0) {
            throw new RuntimeException("No more Flask API retries left. There's probably " +
                "something wrong with the Python server.");
        }

        String urlParameters = "?features_string=" + makeSeparatedFeatureString(features) + "&" +
                makeProbabilisticFlagString(probabilistic);
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
//            logger.info("Response from Python server: {}", responseString);

            // If running a probabilistic classifier, the response will look like '[pNonRel
            // pRel]', since the first class, '0', means "don't keep". Process to just keep the
            // second probability.
            if(probabilistic) {
                String[] responseStringParts = responseString.split("\\s+");
                responseString = responseStringParts[2].trim();
                int endIndex = responseString.length();
                if(responseString.endsWith("]")) {
                    endIndex = endIndex - 1;
                }
                responseString = responseString.substring(0, endIndex);
            }
//            logger.info("Extracted: {}", responseString);

            return Double.parseDouble(responseString);
        }
        catch(SocketTimeoutException timeout) {
            System.err.println("Possible issue with Python API (socket timeout). Retrying " +
                    "connection.");
            timeout.printStackTrace();
            return binClassifyFlask(features, probabilistic, retriesLeft - 1);
        }
        catch(ConnectException connectException) {
            System.err.println("Possible issue with Python API (connect timeout). Retrying " +
                    "connection.");
            connectException.printStackTrace();
            return binClassifyFlask(features, probabilistic, retriesLeft - 1);
        }
    }

    @Override
    public void close() throws IOException {
        stopPythonServer();
    }
}