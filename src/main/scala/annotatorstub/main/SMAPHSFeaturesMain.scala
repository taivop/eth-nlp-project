package annotatorstub.main

import annotatorstub.annotator.smaph.{SmaphSPruner, SmaphSAnnotator}
import annotatorstub.utils.WATRelatednessComputer
import it.unipi.di.acube.batframework.datasetPlugins.DatasetBuilder

object SMAPHSFeaturesMain {

  def main(args: Array[String]) {
//    WATRelatednessComputer.setCache("relatedness.cache")
    // TODO(andrei): Train on both train set A and train set B.
    // TODO(andrei): Check if additional Yahoo data is useful.
    val pruner = SmaphSPruner.trainPruner(DatasetBuilder.getGerdaqTrainA)

    // Note: attempting to train using Smile is very slow and doesn't seem to work right at all,
    // sadly.
//    val pruner = SmaphSPruner.trainPrunerFromCsv("data/all-candidates-4-10-14-10.csv")
//    val annotator = new SmaphSAnnotator(pruner)

  }
}
