package annotatorstub.main;

import annotatorstub.utils.PythonApiInterface;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple test of the Python sklearn API.
 */
public class PythonApiMain {

    public static void main(String[] args) throws IOException {

        List<Double> dummyFeatures = Arrays.asList(1.0, 5.0, 1023.2, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0);

        try(PythonApiInterface classifierApi = new PythonApiInterface(5000)) {
            classifierApi.startPythonServer();
            ArrayList<Double> features = new ArrayList<Double>(dummyFeatures);

            boolean result;

            result = classifierApi.binClassifyFlask(features);
            System.out.println("Result 1: " + result);

            result = classifierApi.binClassifyFlask(features);
            System.out.println("Result 2: " + result);

            try {
                classifierApi.stopPythonServer();
                result = classifierApi.binClassifyFlask(features);  // Should throw an error because server is killed
            }
            catch(ConnectException ex) {
                System.out.println("Server closed successfully.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}