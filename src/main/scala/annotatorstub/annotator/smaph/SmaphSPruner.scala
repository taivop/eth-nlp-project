package annotatorstub.annotator.smaph

import java.io.FileWriter

import annotatorstub.utils.mention.{SmaphCandidate, MentionCandidate}
import it.unipi.di.acube.batframework.data.Annotation
import it.unipi.di.acube.batframework.problems.A2WDataset

import collection.JavaConverters._

import java.util.{HashSet => JHashSet}
import scala.collection.Set


class SmaphSPruner {

//  def train() = ???

  // TODO(andrei): Ensure a model exits (either just computed or loaded from file).
  def shouldKeep(annotation: Annotation): Boolean = ???
}

/**
 * Super lighweight helper class for performing query matchmaking.
 */
case class MentionPosition(start: Int, length: Int)

object MentionPosition {
//  def apply(annotation: Annotation): MentionPosition = MentionPosition(annotation.getPosition, annotation.getLength)
//
//  def apply(candidate: MentionCandidate) = MentionPosition(
//    candidate.getQueryStartPosition,
//    candidate.getQueryEndPosition - candidate.getQueryStartPosition)
}

object SmaphSPruner {

  def savePruner(fileName: String, pruner: SmaphSPruner): Unit = ???

  def loadPruner(fileName: String): SmaphSPruner = ???

  def trainPruner(dataset: A2WDataset): SmaphSPruner = {
    // HERE BE DRAGONS: Make sure that you're not mixing up Java and Scala containers when
    // dealing with inter-language marshalling. Otherwise one might end up spending hours trying
    // to figure out why 'List[Foo]' cannot be converted to 'List[Foo]'.

    val goldStandardJava: List[JHashSet[Annotation]] = dataset.getA2WGoldStandardList.asScala.toList
    val goldStandard: List[Set[Annotation]] = goldStandardJava.map { jHashSet =>
      jHashSet.asScala.toSet
    }

    // flatMap goldStandard into (candidateFeatures from SmaphSAnnotator with
    // corresponding labels).

    // if a generated entity-mention pair is actually present in gold standard, the label is
    // positive, otherwise it's a 0.

    val queries: List[String] = dataset.getTextInstanceList.asScala.toList

    println(s"We have a total of ${queries.length} queries.")
    println(s"Sanity check: gold standard length is ${goldStandard.length}")

    val totalQueries = goldStandard.length
    val queryGroundTruths = queries zip goldStandard

    // TODO(andrei): Move feature creation to separate object.
    val dummyAnnotator = new SmaphSAnnotator(null)

    val start = System.nanoTime()
    queryGroundTruths.zipWithIndex.foreach { case ((query, goldAnnotations), index) =>
      // Get all the candidates and Scalafy them (Java Pair -> Scala Tuple)

      val now = System.nanoTime()
      val elapsedSeconds = (now - start).toDouble / 1000 / 1000 / 1000

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

      val countedCandidates = allCandidates.map { smaphCandidate =>
        val matchedGoldMentions: Int = goldAnnotations.count { goldAnnotation =>
          goldAnnotation.getLength == smaphCandidate.getMentionCandidate.getLength &&
            goldAnnotation.getConcept == smaphCandidate.getEntityID
        }
          // This is just a sanity check
        .ensuring { mentionCount => mentionCount == 0 || mentionCount == 1 }

        (smaphCandidate, matchedGoldMentions)
      }

      // Take the candidates which appear in the gold standard as positive training samples.
      val positiveCandidates = countedCandidates.filter { _._2 > 0 }.map { _._1 }
      val negativeCandidates = countedCandidates.filter { _._2 == 0 }.map { _._1 }

      if (positiveCandidates.size < goldAnnotations.size) {
        // Note: this may happen occasionally, but should not be the norm.
        System
          .err
          .println("There exist gold standard candidates NOT found by our mention entity pair gen.")

        val missing = goldAnnotations.filterNot {
          positiveCandidates.contains(_)
        }
        println(s"Missing IDs that are in the gold standard: ${missing}")
      }
      else if(positiveCandidates.size > goldAnnotations.size) {
        System.err.println("Possible duplicate positives found.")
      }

      // TODO(andrei): Some duplicates seem to be occurring. Make sure you dedupe everything
      // correctly.
      println(s"Found ${positiveCandidates.length} positive candidate(s).")
      println(s"Found ${negativeCandidates.length} negative candidate(s).")

      val csvFileName = "data/all-candidates.csv"
      positiveCandidates.foreach { candidate =>
        dumpTrainingLine(csvFileName, candidate, relevant = true)
      }
      negativeCandidates.foreach { candidate =>
        dumpTrainingLine(csvFileName, candidate, relevant = false)
      }
    }

    new SmaphSPruner
  }

  /**
   * Helper unit which takes a pre-computed training data row consisting of a SmaphCandidate and
   * a flag indicating whether it's relevant or not.
   */
  private def dumpTrainingLine(
    fileName: String,
    smaphCandidate: SmaphCandidate,
    relevant: Boolean
  ): Unit =
    appendCsvLine(fileName, s"${formatCandidate(smaphCandidate)},$relevant\n")

   private def formatCandidate(smaphCandidate: SmaphCandidate): String = {
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

  private def cvSvm(X: Array[Array[Double]], y: Array[Int]) = {
    // TODO(andrei): Compute 3/5-fold CV-error for our classifier, based on the training data.
  }
}
