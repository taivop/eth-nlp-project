package annotatorstub.main.old;

import smile.classification.SVM;
import smile.data.Attribute;
import smile.data.AttributeDataset;
import smile.data.parser.ArffParser;
import smile.data.parser.IOUtils;
import smile.math.kernel.GaussianKernel;
import smile.math.kernel.LinearKernel;
import smile.math.kernel.MercerKernel;
import smile.util.SmileUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

public class JavaSmileExampleMain {

  /** A simple and well-known dataset on which to do some straightforward classification. */
  public static final String IRIS_DATASET_PATH = "./data/misc/iris.arff";

  public static void svmExample() throws Exception {
    ArffParser parser = new ArffParser();

    System.out.println("Loading iris dataset from file: " + IRIS_DATASET_PATH);
    AttributeDataset dataset = parser.parse(IRIS_DATASET_PATH);
    System.out.println("Loaded iris dataset.");

    System.out.println("Attributes:");
    for(Attribute attr : dataset.attributes()) {
      System.out.println("\t- " + attr.getName());
    }

    // This is really ugly to do in Java, but the Scala version is much nicer, and way more
    // readable.
    int trainingCount = dataset.size();
    // Make sure we don't count the label as an attribute.
    int attributeCount = dataset.attributes().length - 1;
    double[][] rawDataset = new double[dataset.size()][attributeCount + 1];
    rawDataset = dataset.toArray(rawDataset);

    double[][] rawX = new double[trainingCount][attributeCount];
    int[]      rawY = new int[trainingCount];
    for(int i = 0; i < trainingCount; ++i) {
      rawX[i] = Arrays.copyOf(rawDataset[i], attributeCount);
      rawY[i] = (int) rawDataset[i][attributeCount];
    }

    double C = 10e-2;
    int classCount = 3;
//    MercerKernel<double[]> kernel = new LinearKernel();
    MercerKernel<double[]> kernel = new GaussianKernel(1.0);
    SVM<double[]> svm = new SVM<>(kernel, C, classCount, SVM.Multiclass.ONE_VS_ALL);
    svm.learn(rawX, rawY);

    // Evaluate training error, since it's not built into the library, apparently.
    int[]    predictY = new int[trainingCount];
    // How the fuck is this library claiming to be so fast when it doesn't seem to have any
    // vectorized computations. HOW?
    int correct = 0;
    for(int i = 0; i < trainingCount; ++i) {
      int prediction = svm.predict(rawX[i]);
      if(prediction == rawY[i]) {
        correct++;
      }
    }

    double trainError = correct * 1.0 / trainingCount;
    System.out.printf("Final training error: %.2f\n", trainError);
  }

  public static void main(String[] args) throws Exception {
    // Don't forget to also grab the sources and JavaDocs for the library when importing it into
    // your project. It's very helpful!

    svmExample();
  }

}
