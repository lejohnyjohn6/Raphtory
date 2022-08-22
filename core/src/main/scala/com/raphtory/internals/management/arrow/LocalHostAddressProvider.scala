package com.raphtory.internals.management.arrow

import com.raphtory.arrowmessaging.ArrowFlightReader
import com.raphtory.arrowmessaging.ArrowFlightServer
import com.raphtory.internals.communication.CanonicalTopic
import com.raphtory.internals.communication.connectors.ArrowFlightHostAddress
import com.raphtory.internals.communication.repositories.ArrowFlightRepository.signatureRegistry
import com.typesafe.config.Config
import org.apache.arrow.memory.RootAllocator

class LocalHostAddressProvider(config: Config, server: ArrowFlightServer)
        extends ArrowFlightHostAddressProvider(config) {

  private val interface = server.getInterface
  private val port      = server.getPort

  override def getAddressAcrossPartitions(topic: String): Map[String, ArrowFlightHostAddress] = addresses.toMap

  override def startAndPublishAddress[T](
      topics: Seq[CanonicalTopic[T]],
      messageHandler: T => Unit
  ): ArrowFlightReader[T] = {
    val stringTopics = topics.map(_.toString).toSet
    stringTopics.foreach(topic => addresses.addOne((topic, ArrowFlightHostAddress(interface, port))))
    val allocator    = new RootAllocator
    ArrowFlightReader(interface, port, allocator, stringTopics, messageHandler, signatureRegistry)
  }
}