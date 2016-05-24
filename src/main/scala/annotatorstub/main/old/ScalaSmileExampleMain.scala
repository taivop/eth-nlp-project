package annotatorstub.main.old

import java.io.{BufferedWriter, File, FileWriter}

import com.thoughtworks.xstream.XStream
import smile.classification._
import smile.data._
import smile.io._
import smile.math.kernel.GaussianKernel
import smile.util.time
import smile.validation._


/**
 * Simple Smile Framework example in Scala, similar to what is done in [[JavaSmileExampleMain]].
 *
 * The code is significantly more concise, highlighting the fact that Smile's API was designed
 * with Scala in mind.
 *
 * To set up a mixed Java-Scala project:
 *    In IntelliJ, simply right-click the project in the project view on the left, select "Add
 *    Framework Support...", and select Scala. If not present in the list, make sure you have the
 *    Scala SDK installed on your machine.
 */
object ScalaSmileExampleMain {

  val IrisDataFile = "data/misc/iris.arff"

  // We can just as well create a dataset from "scratch" on the fly (e.g. from the computed WAT
  // features) and shove that into our classifier directly.
  val classificationData: AttributeDataset = readArff(IrisDataFile, responseIndex = 4)


  def svmExample() = {
    // To get this working, make sure you include the Scala API in the project, not (just) the
    // Java one. While you can use the Java API from Scala, it kinds of defeats the purpose of
    // using Scala to begin with. ;)
    val data = classificationData

    // 'unzipInt' needs 'smile.data._' to be imported for enriching the boring old Java dataset
    // objects.
    val (x, y) = data.unzipInt
//    val kernel = new LinearKernel
    val kernel = new GaussianKernel(0.1)
    val C = 10e-0

    val classifier = svm(x, y, kernel, C)
    val preds: Array[Int] = x.map(classifier.predict)
    val correct = (preds zip y).count { case(predicted, expected) => predicted == expected }
    val trainAccuracy = correct.toDouble / y.length
    val trainError = 1 - trainAccuracy
    println(s"SVM train error ${trainError}")
  }

  /**
   * Computes the CV score of a model, then trains it on all the data and saves it to a file.
   */
  def svmCVExample() = {
    val data = classificationData
    val (x, y) = data.unzipInt
    val kernel = new GaussianKernel(0.75)
    val C = 10e-1

    println("Will perform kfold cross-validation.")
    cv(x, y, k = 5) { case(xfold, yfold) => svm(xfold, yfold, kernel, C) }

    println("Now training model on all data")
    val model = svm(x, y, kernel, C)

    dumpObject("models/svm-iris-example.xml", model)
  }

  /**
   * Dumps a human-readable serialization of the object to the specific file.
   *
   * Warning: if the file exists, this method will overwrite it!
   *
   * While XML might not be the nicest format, this library works out of the box with Smile
   * models, which is the most important thing at the moment.
   */
  def dumpObject(fileName: String, obj: Object): Unit = {
    println(s"Dumping model to file '${fileName}'...")
    val xStream = new XStream()
    xStream.toXML(obj, new FileWriter(fileName))
    println("Dump successful.")
  }

  /**
   * Loads a serialized SVM and uses it to make some predictions.
   */
  def svmFromFileExample() = {
    val modelFile = "models/svm-iris-example.xml"
    println("Will load classifier.")
    val classifier = time {
      new XStream().fromXML(new File(modelFile)).asInstanceOf[SVM[Array[Double]]]
    }
    println("Classifier loaded successfully.")

    val (x, y) = classificationData.unzipInt
    val preds: Array[Int] = x.map(classifier.predict)
    val correct = (preds zip y).count { case(predicted, expected) => predicted == expected }
    val trainAccuracy = correct.toDouble / y.length
    val trainError = 1 - trainAccuracy
    println(s"Reloaded SVM train error ${trainError}")
  }

  /**
   * Helper method to just dump a string to a file.
   */
  def toFile(canonicalFilename: String, text: String): Unit = {
    val file = new File(canonicalFilename)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(text)
    bw.close()
  }

  def main(args: Array[String]) {
//    svmExample()
    svmCVExample()
    svmFromFileExample()
  }

}
