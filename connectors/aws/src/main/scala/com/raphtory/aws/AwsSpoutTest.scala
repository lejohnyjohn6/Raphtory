package com.raphtory.aws

import com.raphtory.Raphtory
import com.raphtory.algorithms.generic.EdgeList
import com.raphtory.aws.graphbuilders.officers.OfficerToCompanyGraphBuilder
import com.raphtory.sinks.FileSink
import com.raphtory.spouts.FileSpout

/**
 * Tests the AWS S3 Spout and Sink, requires bucket name and bucket path that you would like to ingest.
 * Also requires bucket to output results into. Both set in application.conf.
 */

object AwsSpoutTest extends App {

//  val config = Raphtory.getDefaultConfig()
//  val awsS3SpoutBucketName = config.getString("raphtory.spout.aws.local.spoutBucketName")
//  val awsS3SpoutBucketKey = config.getString("raphtory.spout.aws.local.spoutBucketPath")
  //    val awsS3OutputFormatBucketName = config.getString("raphtory.spout.aws.local.outputBucketName")

//      val source               = AwsS3Spout("pometry-data","CompaniesHouse")
  val source = FileSpout("/home/ubuntu/CompaniesHouse/", regexPattern = "^.*\\.([jJ][sS][oO][nN]??)$", recurse = true)
  val builder = new OfficerToCompanyGraphBuilder()
  val output = FileSink("/tmp/coho")
  val graph = Raphtory.load[String](source, builder)
  graph
    .execute(EdgeList())
    .writeTo(output)
  //    Raphtory.streamIO(spout = source, graphBuilder = builder).use { graph =>
  //      IO {
  //        graph
  //          .at(32674)
  //          .past()
  //          .execute(EdgeList())
  //          .writeTo(output)
  //          .waitForJob()
  //        ExitCode.Success
  //      }
  //    }

}
