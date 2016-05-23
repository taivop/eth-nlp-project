package annotatorstub.annotator.smaph

import java.io.FileWriter

import annotatorstub.annotator.wat.HelperWATAnnotator
import annotatorstub.utils.caching.WATRequestCache
import annotatorstub.utils.mention.SmaphCandidate
import it.unipi.di.acube.batframework.data.Annotation
import it.unipi.di.acube.batframework.problems.A2WDataset

import collection.JavaConverters._

import java.util.{HashSet => JHashSet, Optional, Calendar, GregorianCalendar, Date}
import scala.collection.{mutable, Set}

object SmaphSPruning {
  /**
   * Generates Smaph-S training data from the given datasets, and dumps it to a CSV file for later 
   * use.
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

    // Keep track of all exceptions thrown processing individual elements.
    val exceptions = mutable.MutableList.empty[Exception]

    // if a generated entity-mention pair is actually present in gold standard, the label is
    // positive, otherwise it's a 0.

    // All the query texts from all the datasets in a single list.
    val queries: List[String] = datasets.flatMap { _.getTextInstanceList.asScala }.toList

    println(s"We have a total of ${queries.length} queries from ${datasets.length} datasets:")
    datasets
      .map { ds => (ds.getName, ds.getSize) }
      .zipWithIndex
      .map { case ((name, size), i) => s" - $i $name ($size queries)" }
      . foreach { println(_) }
    println(s"Sanity check: gold standard length is ${goldStandard.length}")

    val totalQueries = goldStandard.length
    val queryGroundTruths = queries zip goldStandard
    val watRequestCache = new WATRequestCache("watapi.cache", "Full WAT API cache.", 1000)

    // TODO(andrei): Move feature creation to separate object.
    // Right now we create an annotator just to use it for feature generation.
    val dummyAnnotator = new SmaphSAnnotator(new SmaphSNoPruning, false, watRequestCache)

    // The file where we will be saving our training data for safe keeping.
    val csvFileName = genCsvFileName()
    val start = System.nanoTime()

      queryGroundTruths
      .zipWithIndex
      .foreach { case ((query, goldAnnotations), index) =>
        try {
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
          goldAnnotations.map {
            "\t" + _.getConcept
          } foreach println

          val matchedCandidates = allCandidates.map { smaphCandidate =>
            // For every generated candidate, see if it matches anything in the gold standard.
            val matchedGoldMention: Boolean = goldAnnotations.exists { goldAnnotation =>
              goldAnnotation.getPosition == smaphCandidate
                .getMentionCandidate
                .getQueryStartPosition &&
                goldAnnotation.getLength == smaphCandidate.getMentionCandidate.getLength &&
                goldAnnotation.getConcept == smaphCandidate.getEntityID
            }

            (smaphCandidate, matchedGoldMention)
          }

          // Take the candidates which appear in the gold standard as positive training samples.
          val positiveCandidates = matchedCandidates.filter { case (_, inGold) => inGold }.map {
            _._1
          }
          val negativeCandidates = matchedCandidates.filter { case (_, inGold) => !inGold }.map {
            _._1
          }

          if (positiveCandidates.size < goldAnnotations.size) {
            // Note: this may happen occasionally, but should not be the norm.
            System
              .err
              .println(
                "There exist gold standard candidates NOT found by our mention entity pair gen.")

            val missing = goldAnnotations.filterNot {
              positiveCandidates.contains(_)
            }
            println(
              "Missing IDs that are in the gold standard:",
              missing.map {
                _.getConcept
              }.mkString(","))
          }
          else if (positiveCandidates.size > goldAnnotations.size) {
            System.err.println(
              "\nWARNING: Possible duplicate positives found. This may disturb your " +
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
        }
        catch {
          case e: Exception =>
            System.err.println("Caught weird exception while generating CSV. Ignoring for now and" +
              " moving to next element.")
            e.printStackTrace(System.err)
            exceptions += e
        }
    }

    dummyAnnotator.getAuxiliaryAnnotator.asInstanceOf[HelperWATAnnotator].getRequestCache.flush()
    println(s"Possible duplicate data points generated: $posDupes")
    println(s"Found ${exceptions.size} exceptions processing dudes:")
    exceptions.map { e => s"\t-${e.getMessage}" } foreach println

    println("All done!")
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
     // TODO(andrei): Escape commas in mention; low priority since this doesn't happen in the
     // Gerdaq datasets.
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
