package com.raphtory.arrowmessaging

import cats.effect.Resource
import cats.effect.Sync
import org.apache.arrow.flight.FlightServer
import org.apache.arrow.flight.Location
import org.apache.arrow.memory.BufferAllocator
import org.apache.logging.log4j.LogManager

import java.net._
import java.io.IOException
import java.util.concurrent.Executors

class ArrowFlightServer(allocator: BufferAllocator) {
  private val logger = LogManager.getLogger(classOf[ArrowFlightServer])

  private var started = false

  private val location       = Location.forGrpcInsecure(InetAddress.getLocalHost.getHostAddress, 0)
  private val flightProducer = new ArrowFlightProducer(allocator, location)

  private val flightServer =
    FlightServer
      .builder(
              allocator,
              location,
              flightProducer
      )
      .build()

  flightServer.synchronized {
    flightServer.start()
    started = true
    flightServer.notify()
  }
  logger.info("ArrowFlightServer({},{}) is online", flightServer.getLocation.getUri.getHost, flightServer.getPort)
  //flightServer.awaitTermination()

//  def waitForServerToStart(): Unit =
//    flightServer.synchronized {
//      while (!started)
//        try flightServer.wait()
//        catch {
//          case e: InterruptedException =>
//            e.printStackTrace()
//        }
//    }

  def getInterface: String = flightServer.getLocation.getUri.getHost

  def getPort: Int = flightServer.getPort

  def close(): Unit =
    try {
      flightServer.shutdown()
      logger.debug("Flight server closed")
      flightProducer.close()
      logger.debug("Flight producer closed")
    }
    catch {
      case e: Exception =>
        e.printStackTrace()
    }
    finally {}

  override def toString: String = s"ArrowFlightServer($getInterface,$getPort)"
}

object ArrowFlightServer {

  def apply[IO[_]](allocator: BufferAllocator)(implicit IO: Sync[IO]): Resource[IO, ArrowFlightServer] =
    for {
      arrowServer <- Resource.make(IO.delay(new ArrowFlightServer(allocator)))(server => IO.delay(server.close()))
    } yield arrowServer

}
