package annotatorstub.main

import annotatorstub.annotator.smaph.{SmaphSPruner, SmaphSAnnotator}
import annotatorstub.utils.WATRelatednessComputer
import it.unipi.di.acube.batframework.datasetPlugins.DatasetBuilder

object SMAPHSFeaturesMain {

  def main(args: Array[String]) {
    WATRelatednessComputer.setCache("relatedness.cache")
    val annotator = new SmaphSAnnotator()

//    annotator.getCandidatesWithFeatures("neil armstrong moon landing")

    // TODO(andrei): Train on both train set A and train set B.
    // TODO(andrei): Check if additional Yahoo data is useful.
    val pruner = SmaphSPruner.trainPruner(DatasetBuilder.getGerdaqTrainA)
  }
}
