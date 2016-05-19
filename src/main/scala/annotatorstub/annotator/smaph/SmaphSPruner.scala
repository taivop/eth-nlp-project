package annotatorstub.annotator.smaph

import java.io.FileWriter

import annotatorstub.annotator.wat.HelperWATAnnotator
import annotatorstub.utils.mention.{SmaphCandidate, MentionCandidate}
import it.unipi.di.acube.batframework.data.Annotation
import it.unipi.di.acube.batframework.problems.A2WDataset
import it.unipi.di.acube.batframework.systemPlugins.WATAnnotator

import smile.classification._
import smile.data._
import smile.io._
import smile.math.kernel.LinearKernel
import smile.validation._

import collection.JavaConverters._

import java.util.{HashSet => JHashSet, Optional, Calendar, GregorianCalendar, Date}
import scala.collection.Set

object SmaphSPruner {
  /**
   * Generates training data from the given datasets, and dumps it to a CSV file for later use.
   *
   * Note: SVM training not attempted, as is currently VERY slow using Smile.
   */
  def genPrunerData(datasets: A2WDataset*): Unit = {
    // HERE BE DRAGONS: Make sure that you're not mixing up Java and Scala containers when
    // dealing with inter-language marshalling. Otherwise one might end up spending hours trying
    // to figure out why 'List[Foo]' cannot be converted to 'List[Foo]'.

    val goldStandardJava: List[JHashSet[Annotation]] = datasets
      .flatMap { _.getA2WGoldStandardList.asScala }
      .toList

    val goldStandard: List[Set[Annotation]] = goldStandardJava.map { jHashSet =>
      jHashSet.asScala.toSet
    }

    // The number of times we encountered possible duplicates when generating training data. A
    // small number should be ok, as sometimes words are repeated in a query (e.g. "how to tie
    // a tie").
    var posDupes: Int = 0

    // if a generated entity-mention pair is actually present in gold standard, the label is
    // positive, otherwise it's a 0.

    // All the query texts from all the datasets in a single list.
    val queries: List[String] = datasets.flatMap { _.getTextInstanceList.asScala }.toList

    println(s"We have a total of ${queries.length} queries from ${datasets.length} datasets:")
    datasets.map { _.getName }.zipWithIndex.map { case (name, i) => s" - $i $name" }  foreach println
    println(s"Sanity check: gold standard length is ${goldStandard.length}")

    val totalQueries = goldStandard.length
    val queryGroundTruths = queries zip goldStandard

    // TODO(andrei): Move feature creation to separate object.
    // Right now we create an annotator just to use it for feature generation.
    val dummyAnnotator = new SmaphSAnnotator(Optional.empty[Smaph1Pruner]())

    // The file where we will be saving our training data for safe keeping.
    val csvFileName = genCsvFileName()
    val start = System.nanoTime()

//    val allTrainingData: List[(SmaphCandidate, Boolean)] = queryGroundTruths
      queryGroundTruths
      .zipWithIndex
      .foreach { case ((query, goldAnnotations), index) =>
      val now = System.nanoTime()
      val elapsedSeconds = (now - start).toDouble / 1000 / 1000 / 1000

      // Get all the candidates and Scalafy them (Java Pair -> Scala Tuple).
      val allCandidates: List[SmaphCandidate] = dummyAnnotator
        .getCandidatesWithFeatures(query)
        .asScala
        .toList

      println()
      println(s"Query [${index + 1}/$totalQueries]: $query")
      println(f"Elapsed time: $elapsedSeconds%2.2f s")
      println(s"All computed candidates: ${allCandidates.length}")
      println(s"Gold standard has: ${goldAnnotations.size}")
      // TODO(andrei): Find nice way of getting the actual entity title, for more informative data dumps.
      goldAnnotations.map {
        "\t" + _.getConcept
      } foreach println

      val matchedCandidates = allCandidates.map { smaphCandidate =>
        // For every generated candidate, see if it matches anything in the gold standard.
        val matchedGoldMention: Boolean = goldAnnotations.exists { goldAnnotation =>
          goldAnnotation.getPosition == smaphCandidate.getMentionCandidate.getQueryStartPosition &&
          goldAnnotation.getLength == smaphCandidate.getMentionCandidate.getLength &&
          goldAnnotation.getConcept == smaphCandidate.getEntityID
        }

        (smaphCandidate, matchedGoldMention)
      }

      // Take the candidates which appear in the gold standard as positive training samples.
      val positiveCandidates = matchedCandidates.filter { case (_, inGold) =>   inGold }.map { _._1 }
      val negativeCandidates = matchedCandidates.filter { case (_, inGold) => ! inGold }.map { _ ._1 }

      if (positiveCandidates.size < goldAnnotations.size) {
        // Note: this may happen occasionally, but should not be the norm.
        System
          .err
          .println("There exist gold standard candidates NOT found by our mention entity pair gen.")

        val missing = goldAnnotations.filterNot {
          positiveCandidates.contains(_)
        }
        println(s"Missing IDs that are in the gold standard:",
          missing.map { _.getConcept }.mkString(","))
      }
      else if(positiveCandidates.size > goldAnnotations.size) {
        System.err.println("\nWARNING: Possible duplicate positives found. This may disturb your " +
          "SVM.")
        posDupes += 1
      }

      println(s"Found ${positiveCandidates.length} positive candidate(s).")
      println(s"Found ${negativeCandidates.length} negative candidate(s).")

      // Dumps all the labeled training information into a CSV file, for later inspection and
      // training.
      positiveCandidates.foreach { candidate =>
        dumpTrainingLine(csvFileName, candidate, relevant = true)
      }
      negativeCandidates.foreach { candidate =>
        dumpTrainingLine(csvFileName, candidate, relevant = false)
      }

//      positiveCandidates.map { c => (c, true) } ++ negativeCandidates.map { c => (c, false) }
    }

    dummyAnnotator.getAuxiliaryAnnotator.asInstanceOf[HelperWATAnnotator].getRequestCache.flush()
    println(s"Possible duplicate data points generated: $posDupes")
  }

  /**
   * Loads pre-computed training data from the specified CSV file.
   */
  def loadTrainingData(csvFileName: String): List[(SmaphCandidate, Boolean)] =
    scala.io.Source.fromFile(csvFileName).getLines.map(parseCsvLine).toList

  val ExpectedLineLength = 18
  // TODO(andrei): Keep this up to date as we add more and more features into our pipeline.
  val FeatureCount = 10

  def parseCsvLine(line: String): (SmaphCandidate, Boolean) = {
    throw new RuntimeException("This method is deprecated. Please use Python.")

    val segments = line.split("\\s*,\\s*").ensuring(
      _.length == ExpectedLineLength,
      s"Bad component length in line: ${line} (expected $ExpectedLineLength)")

    // Sample line (May 10):
    // 364646, lumet familt, 7, 19, 12, featureStart, [feature_count features], featureEnd, true
    val smaphCandidate = new SmaphCandidate(
      segments(0).toInt,
      new MentionCandidate(segments(2).toInt, segments(3).toInt, segments(1)),
      segments.slice(6, 6 + FeatureCount)
        // Convert the strings to doubles.
        .map { _.toDouble }
        // Fix up bad values.
        .map { feature: Double =>
          if (feature.isInfinite || feature.isNaN)
            0.0
          else
            feature
        }
        // Convert to the expected Java format.
        .map { raw => java.lang.Double.valueOf(raw) }
        .toList
        .asJava
    ).ensuring { candidate => candidate.getFeatures.size() == FeatureCount }

    val relevance = segments(segments.length - 1).toBoolean

    (smaphCandidate, relevance)
  }

  /**
   * Helper function which takes a pre-computed training data row consisting of a SmaphCandidate and
   * a flag indicating whether it's relevant or not, and dumps it to a CSV file which can be
   * reloaded in Java or Scala, as well as any other ML environment you could think of (e.g.
   * Python+sklearn).
   */
  private def dumpTrainingLine(
    fileName: String,
    smaphCandidate: SmaphCandidate,
    relevant: Boolean
  ): Unit =
    appendCsvLine(fileName, s"${formatCandidate(smaphCandidate)},$relevant\n")

   private def formatCandidate(smaphCandidate: SmaphCandidate): String = {
     // TODO(andrei): Escape commas in mention.
     val featureString = smaphCandidate.getFeatures.asScala.toList.mkString(", ")
     val mc = smaphCandidate.getMentionCandidate
     s"${smaphCandidate.getEntityID}, ${mc.getMention}, ${mc.getQueryStartPosition}, " +
       s"${mc.getQueryEndPosition}, ${mc.getLength}, " +
       s"featureStart, $featureString, featureEnd"
   }

  private def appendCsvLine(fileName: String, line: String): Unit = {
    val writer = new FileWriter(fileName, true)
    writer.append(line)
    writer.close()
  }

  private def genCsvFileName(): String = {
    val now: Calendar = new GregorianCalendar
    val day = "%02d" format now.get(Calendar.DAY_OF_MONTH)
    val month = "%02d" format now.get(Calendar.MONTH) + 1
    val hour = "%02d" format now.get(Calendar.HOUR_OF_DAY)
    val min = "%02d" format now.get(Calendar.MINUTE)

    s"data/all-candidates-$month-$day-$hour-$min.csv"
  }
}
