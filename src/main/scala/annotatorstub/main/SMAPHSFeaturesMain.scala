package annotatorstub.main

import annotatorstub.annotator.smaph.{SmaphSPruning, SmaphSAnnotator}
import annotatorstub.utils.WATRelatednessComputer
import it.unipi.di.acube.batframework.datasetPlugins
import it.unipi.di.acube.batframework.datasetPlugins.DatasetBuilder

object SMAPHSFeaturesMain {

  def main(args: Array[String]) {
    // This only generates the CSV dump.
    SmaphSPruning.genPrunerData(
      DatasetBuilder.getGerdaqTrainA,
      DatasetBuilder.getGerdaqTrainB,
      // Don't forget to train on this as well, since at this point we're just using Gerdaq-Test
      // for validation.
      DatasetBuilder.getGerdaqDevel
      // This is our secret weapon.
      // Disregard that, we can't use it since it's also what our final evaluation will be on. :(
//      ,WebScopeXMLProcessing.getWebscope
    )
  }
}
