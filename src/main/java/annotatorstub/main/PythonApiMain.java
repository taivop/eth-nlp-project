package annotatorstub.main;

import annotatorstub.utils.PythonApiInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


public class PythonApiMain {

    public static void main(String[] args) throws IOException {
        PythonApiInterface classifierApi = new PythonApiInterface(5000);

        try {
            classifierApi.startPythonServer();
            ArrayList<Double> features = new ArrayList<Double>(Arrays.asList(1.0, 5.0, 1023.2));

            boolean result;

            result = classifierApi.binClassifyFlask(features);
            System.out.println("Result 1: " + result);          // Should print the sum of features

            classifierApi.stopPythonServer();

            result = classifierApi.binClassifyFlask(features);  // Should throw an error because server is killed
            System.out.println("Result 2: " + result);
        } catch (Exception e) {
            classifierApi.stopPythonServer();
            e.printStackTrace();
        }


    }
}