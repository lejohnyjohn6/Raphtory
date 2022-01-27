package com.raphtory.algorithms.generic.centrality

import com.raphtory.core.model.algorithm.{GraphAlgorithm, GraphPerspective, Row, Table}

class OutDegreeAverage(output:String) extends GraphAlgorithm {
  override def tabularise(graph: GraphPerspective): Table = {
    val nodeCount = graph.nodeCount()
    graph
      .select({
        vertex =>
          val sized =
            try vertex.getOutEdges().size.toDouble / nodeCount.toDouble
            catch {
              case _: ArithmeticException => 0
            }
          Row(vertex.name(), sized)
      })
  }

  override def write(table: Table): Unit = {
    table.writeTo(output)
  }
}


object OutDegreeAverage{
  def apply(output:String= "/tmp/outDegreeAverage") = new OutDegreeAverage(output)
}

