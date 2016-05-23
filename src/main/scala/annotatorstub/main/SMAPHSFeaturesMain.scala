package annotatorstub.main

import annotatorstub.annotator.smaph.{SmaphSPruner, SmaphSAnnotator}
import annotatorstub.utils.WATRelatednessComputer
import it.unipi.di.acube.batframework.datasetPlugins
import it.unipi.di.acube.batframework.datasetPlugins.DatasetBuilder

object SMAPHSFeaturesMain {

  def main(args: Array[String]) {
    // This only generates the CSV dump.
    SmaphSPruner.genPrunerData(
      DatasetBuilder.getGerdaqTrainA,
      DatasetBuilder.getGerdaqTrainB,
      DatasetBuilder.getGerdaqDevel
      // This is our secret weapon.
      // Disregard that, we can't use it since it's also what our final evaluation will be on. :(
//      ,WebScopeXMLProcessing.getWebscope
    )
  }
}
