package annotatorstub.main

import annotatorstub.annotator.smaph.{SmaphSPruner, SmaphSAnnotator}
import annotatorstub.utils.WATRelatednessComputer
import it.unipi.di.acube.batframework.datasetPlugins.DatasetBuilder

object SMAPHSFeaturesMain {

  def main(args: Array[String]) {
    // TODO(andrei): Check if additional Yahoo data is useful.

    // This only generates the CSV dump.
    val pruner = SmaphSPruner.genPrunerData(DatasetBuilder.getGerdaqTrainA, DatasetBuilder.getGerdaqTrainB)
  }
}
