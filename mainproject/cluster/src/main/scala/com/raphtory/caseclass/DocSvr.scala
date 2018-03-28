package com.raphtory.caseclass

import akka.actor.{ActorSystem, ExtendedActorSystem}
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}

import scala.collection.JavaConversions
import scala.collection.JavaConversions._

trait DocSvr {
  def seedLoc:String
  val clusterSystemName = "dockerexp"
  val ssn = java.util.UUID.randomUUID.toString
  implicit val system:ActorSystem

  def init( seeds:List[String] ) : ActorSystem = {

    val config = ConfigFactory.load()

    config.withValue("akka.remote.netty.tcp.bind-hostname", ConfigValueFactory.fromAnyRef(java.net.InetAddress.getLocalHost().getHostAddress())) //docker
      //.withValue("akka.remote.netty.tcp.bind-hostname", ConfigValueFactory.fromAnyRef(ipAndPort.hostIP)) //singularirty (maybe "192.168.1.1")
      .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(java.net.InetAddress.getByName(config.getString("settings.ip")).getHostAddress()))
      .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(config.getInt("settings.bport")))
      .withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(JavaConversions.asJavaIterable(seeds.map(_ => s"akka.tcp://$clusterSystemName@$seedLoc").toIterable)))

    val actorSystem = ActorSystem( clusterSystemName, config )
    printConfigInfo(config,actorSystem)
    actorSystem // return actor system with set config
  }

  var nodes = Set.empty[akka.cluster.Member]  // cluster node registry
  def getNodes() = nodes.map( m => m.address+" "+m.getRoles.mkString ).mkString(",")
  def printConfigInfo(config:Config, as:ActorSystem) ={
    println(s"------ $ssn ------")
    println(s"Binding core internally on ${config.getString("akka.remote.netty.tcp.bind-hostname")} port ${config.getString("akka.remote.netty.tcp.bind-port")}")
    println(s"Binding core externally on ${config.getString("akka.remote.netty.tcp.hostname")} port ${config.getString("akka.remote.netty.tcp.port")}")
    println(s"Seeds: ${config.getList("akka.cluster.seed-nodes").toList}")
    println("Roles: "+config.getList("akka.cluster.roles").toList)
    println("My Akka URI: " + as.asInstanceOf[ExtendedActorSystem].provider.getDefaultAddress)  // warning: unorthodox mechanism
  }
}
