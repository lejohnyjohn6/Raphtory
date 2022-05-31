package com.raphtory.examples.twitter.higgsdataset

import com.raphtory.algorithms.generic.centrality.PageRank
import com.raphtory.deployment.Raphtory
import com.raphtory.examples.twitter.higgsdataset.analysis.{MemberRank, TemporalMemberRank}
import com.raphtory.examples.twitter.higgsdataset.graphbuilders.TwitterGraphBuilder
import com.raphtory.output.PulsarOutputFormat
import com.raphtory.spouts.FileSpout
import com.raphtory.util.FileUtils

object Runner extends App {

  val path = "/tmp/higgs-retweet-activity.csv"
  val url = "https://raw.githubusercontent.com/Raphtory/Data/main/higgs-retweet-activity.csv"
  FileUtils.curlFile(path, url)

  // Create Graph
  val source = FileSpout(path)
  val builder = new TwitterGraphBuilder()
  val graph = Raphtory.stream(spout = source, graphBuilder = builder)
  Thread.sleep(20000)
  val output = PulsarOutputFormat("Retweets")

  graph
    .range(1341101181, 1341705593, 500000000)
    .past()
    .transform(PageRank())
    .execute(MemberRank() -> TemporalMemberRank())
    .writeTo(output)
}