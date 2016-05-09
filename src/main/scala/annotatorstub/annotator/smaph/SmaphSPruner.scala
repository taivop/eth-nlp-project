package annotatorstub.annotator.smaph

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

    new SmaphSPruner
  }
}
