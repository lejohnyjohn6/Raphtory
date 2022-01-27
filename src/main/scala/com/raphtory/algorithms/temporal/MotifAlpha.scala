package com.raphtory.algorithms.temporal

import com.raphtory.core.model.algorithm.{GraphAlgorithm, GraphPerspective, Row, Table}

/**
Description
  The algorithms identifies 2-edge-1-node temporal motifs; It detects one type of motifs:
  For each incoming edge a vertex has, the algorithm checks whether there are any outgoing 
  edges which occur after it and returns a count of these.
  
Parameters
  fileOutput (String) : The path where the output will be saved. If not specified, defaults to /tmp/PageRank

Returns
  ID (Long) : Vertex ID
  Number of Type1 motifs (Long) : Number of Type-1 motifs.
**/
class MotifAlpha(fileOutput:String="/tmp/motif_alpha") extends GraphAlgorithm {

  override def tabularise(graph: GraphPerspective): Table = {
    graph
      .select({
        vertex =>
          if (vertex.explodeInEdges().nonEmpty & vertex.explodeOutEdges().nonEmpty) {
            val out = vertex.explodeInEdges()
              .map(inEdge => vertex.explodeOutEdges()
                .filter(e => e.getTimestamp() > inEdge.getTimestamp() & e.dst() != inEdge.src() & e.dst() != inEdge.dst()).size).sum
            Row(vertex.name(), out)
          }
          else {
            Row(vertex.name(), 0)
          }
      }
      )
  }

  override def write(table: Table): Unit = {
    table.writeTo(fileOutput)
  }

}

object MotifAlpha{
  def apply(path:String) = new MotifAlpha(path)
}

