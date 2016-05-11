package annotatorstub.main;

import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class JavaWekaExampleMain {

    public static void wekaClassification() throws IOException {
        try {
            ConverterUtils.DataSource source = new ConverterUtils.DataSource("data/misc/iris.arff");
            Instances data = source.getDataSet();
            // setting class attribute if the data format does not provide this information
            // For example, the XRFF format saves the class attribute information as well
            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }

            System.out.println(data);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            // Gotta love Weka's amazing 'throws Exception' API.
            System.err.println("Something went terribly wrong with Weka.");
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        try {
            wekaClassification();
        }
        catch(IOException ex) {
            throw new RuntimeException("Could not run Weka example.", ex);
        }
    }
}
