package com.raphtory.dev.wordSemantic.graphbuilders

import com.raphtory.core.components.graphbuilder.GraphBuilder
import com.raphtory.core.implementations.pojograph.messaging._
import com.raphtory.core.model.graph.{FloatProperty, Properties, StringProperty}
import com.raphtory.dev.wordSemantic.spouts.Update


class CoMatParquetGB extends GraphBuilder[Update] {

  override def parseTuple(row: Update) =
    try {
      val time  = row._1
      val src   = row._2
      val dst   = row._3
      val srcID = assignID(src)
      val dstID = assignID(dst)
      val freq  = row._4

      addVertex(updateTime = time, srcId = srcID, Properties(StringProperty("Word", src)))
      addVertex(updateTime = time, srcId = dstID, Properties(StringProperty("Word", dst)))
      addEdge(
                      updateTime = time,
                      srcId = srcID,
                      dstId = dstID,
                      Properties(FloatProperty("Frequency", freq.toFloat))
              )
    } catch {
      case e: Exception => println(e, row)
    }
}