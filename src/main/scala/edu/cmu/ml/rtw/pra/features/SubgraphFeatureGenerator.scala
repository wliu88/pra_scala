package edu.cmu.ml.rtw.pra.features

import edu.cmu.ml.rtw.pra.config.PraConfig
import edu.cmu.ml.rtw.pra.config.JsonHelper
import edu.cmu.ml.rtw.pra.experiments.Dataset
import edu.cmu.ml.rtw.users.matt.util.Dictionary
import edu.cmu.ml.rtw.users.matt.util.FileUtil
import edu.cmu.ml.rtw.users.matt.util.Pair
import edu.cmu.ml.rtw.users.matt.util.Vector

import java.io.File

import scala.collection.JavaConverters._
import scala.collection.mutable

import org.json4s._
import org.json4s.native.JsonMethods._

class SubgraphFeatureGenerator(
    params: JValue,
    praBase: String,
    config: PraConfig,
    fileUtil: FileUtil = new FileUtil()) extends FeatureGenerator {
  implicit val formats = DefaultFormats
  val featureParamKeys = Seq("type", "path finder", "feature extractors", "feature size")
  JsonHelper.ensureNoExtras(params, "pra parameters -> features", featureParamKeys)

  type Subgraph = java.util.Map[PathType, java.util.Set[Pair[Integer, Integer]]]
  val featureDict = new Dictionary
  val featureSize = JsonHelper.extractWithDefault(params, "feature size", -1)

  override def createTrainingMatrix(data: Dataset): FeatureMatrix = {
    createMatrixFromData(data)
  }

  override def removeZeroWeightFeatures(weights: Seq[Double]): Seq[Double] = weights

  override def createTestMatrix(data: Dataset): FeatureMatrix = {
    val testMatrix = createMatrixFromData(data)
    if (config.outputBase != null) {
      val output = config.outputBase + "test_matrix.tsv"
      config.outputter.outputFeatureMatrix(output, testMatrix, getFeatureNames().toList.asJava)
    }
    testMatrix
  }

  def createMatrixFromData(data: Dataset) = {
    val subgraphs = getLocalSubgraphs(data)
    extractFeatures(subgraphs)
  }

  override def getFeatureNames(): Array[String] = {
    // Not really sure if par is useful here...  Maybe I should just take it out.
    ("bias" +: (1 until featureDict.getNextIndex).par.map(i => featureDict.getString(i)).seq).toArray
  }

  val featureExtractors = createExtractors(params)

  def getLocalSubgraphs(data: Dataset): Map[(Int, Int), Subgraph] = {
    println("Finding local subgraphs with " + data.getAllSources().size() + " training instances")

    // First we get necessary path finding parameters from the params object (we do this here
    // because the params object is hard to work with in java; otherwise we'd just pass part of the
    // object to the path finder).
    val finderParams = params \ "path finder"
    val finderParamKeys = Seq("walks per source", "path finding iterations")
    JsonHelper.ensureNoExtras(finderParams, "pra parameters -> features -> path finder", finderParamKeys)
    val walksPerSource = JsonHelper.extractWithDefault(finderParams, "walks per source", 100)
    val numIters = JsonHelper.extractWithDefault(finderParams, "path finding iterations", 3)

    // Now we create and run the path finder.
    val edgesToExclude = createEdgesToExclude(data, config.unallowedEdges)
    val finder = new PathFinder(config.graph,
      config.numShards,
      data.getAllSources(),
      data.getAllTargets(),
      new SingleEdgeExcluder(edgesToExclude),
      walksPerSource,
      PathTypePolicy.PAIRED_ONLY,
      new BasicPathTypeFactory)
    finder.execute(numIters)
    // This seems to be necessary on small graphs, at least, and maybe larger graphs, for some
    // reason I don't understand.
    Thread.sleep(500)

    finder.getLocalSubgraphs.asScala.map(entry =>
        ((entry._1.getLeft.toInt, entry._1.getRight.toInt), entry._2)).toMap
  }

  def extractFeatures(subgraphs: Map[(Int, Int), Subgraph]): FeatureMatrix = {
    val matrix_rows = subgraphs.par.flatMap(entry => {
      val source = entry._1._1
      val target = entry._1._2
      val subgraph = entry._2
      val features = featureExtractors.flatMap(_.extractFeatures(source, target, subgraph).asScala)
      if (features.size > 0) {
        Seq(createMatrixRow(source, target, features.toSet.map(hashFeature).toSeq))
      } else {
        Seq()
      }
    }).seq.toList
    new FeatureMatrix(matrix_rows.asJava)
  }

  def createExtractors(params: JValue): Seq[FeatureExtractor] = {
    val extractorNames = JsonHelper.extractWithDefault(params, "feature extractors",
      List("PraFeatureExtractor"))
    extractorNames.map(_ match {
      case "PraFeatureExtractor" => new PraFeatureExtractor(config.edgeDict)
      case "OneSidedFeatureExtractor" => new OneSidedFeatureExtractor(config.edgeDict, config.nodeDict)
      case "CategoricalComparisonFeatureExtractor" => new CategoricalComparisonFeatureExtractor(config.edgeDict, config.nodeDict)
      case other => throw new IllegalStateException(s"Unrecognized feature extractor: $other")
    })
  }

  def hashFeature(feature: String): Int = {
    if (featureSize == -1) {
      featureDict.getIndex(feature)
    } else {
      val hash = feature.hashCode % featureSize
      if (hash >= 0)
        hash
      else
        hash + featureSize
    }
  }

  def createMatrixRow(source: Int, target: Int, features: Seq[Int]): MatrixRow = {
    val values = new Array[Double](features.size + 1)
    for (i <- 0 to features.size) {
      values(i) = 1
    }
    new MatrixRow(source, target, (features :+ 0).toArray, values)
  }
}
