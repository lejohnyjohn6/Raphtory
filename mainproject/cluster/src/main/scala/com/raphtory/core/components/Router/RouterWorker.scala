package com.raphtory.core.components.Router

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator
import com.raphtory.core.components.Router.RouterWorker.CommonMessage.TimeBroadcast
import com.raphtory.core.model.communication._
import com.raphtory.core.utils.Utils
import com.raphtory.core.utils.Utils.getManager
import kamon.Kamon

import scala.collection.parallel.mutable.ParTrieMap
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.hashing.MurmurHash3

// TODO Add val name which sub classes that extend this trait must overwrite
//  e.g. BlockChainRouter val name = "Blockchain Router"
//  Log.debug that read 'Router' should then read 'Blockchain Router'
abstract class RouterWorker[In <: SpoutGoing](val routerId: Int, val workerID: Int, val initialManagerCount: Int)
        extends Actor
        with ActorLogging {
  implicit val executionContext: ExecutionContext = context.system.dispatcher

  private val messageIDs = ParTrieMap[String, Int]()

  private val routerWorkerUpdates =
    Kamon.counter("Raphtory_Router_Output").withTag("Router", routerId).withTag("Worker", workerID)

  // todo: wvv let people know parseTuple will create a list of update message
  //  and this trait will handle logic to send to graph
  protected def parseTuple(value: In): List[GraphUpdate]

  final protected val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Put(self)

  override def preStart(): Unit = {
    log.debug(s"RouterWorker [$routerId] is being started.")
    context.system.scheduler
      .schedule(initialDelay = 5.seconds, interval = 5.second, receiver = self, message = TimeBroadcast)
  }

  override def receive: Receive = work(initialManagerCount, 0L, 0L)

  private def work(managerCount: Int, trackedTime: Long, newestTime: Long): Receive = {
    case msg: UpdatedCounter =>
      log.debug(s"RouterWorker [$routerId] received [$msg] request.")
      if (managerCount < msg.newValue) context.become(work(msg.newValue, trackedTime, newestTime))

    case AllocateTuple(record: In) => //todo: wvv AllocateTuple should hold type of record instead of using Any
      log.debug(s"RouterWorker [$routerId] received AllocateTuple[$record] request.")
      parseTupleAndSendGraph(record, managerCount, false, trackedTime).foreach(newNewestTime =>
        context.become(work(managerCount, trackedTime, newNewestTime))
      )

    case msg @ AllocateTrackedTuple(
                wallClock,
                record: In
        ) => //todo: wvv AllocateTrackedTuple should hold type of record instead of using Any
      log.debug(s"RouterWorker [$routerId] received [$msg] request.")
      val newNewestTime = parseTupleAndSendGraph(record, managerCount, true, wallClock).getOrElse(newestTime)
      context.become(work(managerCount, wallClock, newNewestTime))

    case TimeBroadcast =>
      Utils.getAllWriterWorkers(managerCount).foreach { workerPath =>
        mediator ! DistributedPubSubMediator.Send(
                workerPath,
                RouterWorkerTimeSync(newestTime, s"${routerId}_$workerID", getMessageIDForWriter(workerPath)),
                false
        )
      }
    case unhandled => log.warning(s"RouterWorker received unknown [$unhandled] message.")
  }

  protected def assignID(uniqueChars: String): Long = MurmurHash3.stringHash(uniqueChars)

  private def parseTupleAndSendGraph(
      record: In,
      managerCount: Int,
      trackedMessage: Boolean,
      trackedTime: Long
  ): Option[Long] =
    parseTuple(record).map(update => sendGraphUpdate(update, managerCount, trackedMessage, trackedTime)).lastOption

  private def sendGraphUpdate(
      message: GraphUpdate,
      managerCount: Int,
      trackedMessage: Boolean,
      trackedTime: Long
  ): Long = {
    routerWorkerUpdates.increment()

    val path             = getManager(message.srcID, managerCount)
    val id               = getMessageIDForWriter(path)
    val trackedTimeToUse = if (trackedMessage) trackedTime else -1L

    val sentMessage = message match {
      case m: VertexAdd =>
        TrackedVertexAdd(s"${routerId}_$workerID", id, trackedTimeToUse, m)
      case m: VertexAddWithProperties =>
        TrackedVertexAddWithProperties(s"${routerId}_$workerID", id, trackedTimeToUse, m)
      case m: EdgeAdd =>
        TrackedEdgeAdd(s"${routerId}_$workerID", id, trackedTimeToUse, m)
      case m: EdgeAddWithProperties =>
        TrackedEdgeAddWithProperties(s"${routerId}_$workerID", id, trackedTimeToUse, m)
      case m: VertexDelete =>
        TrackedVertexDelete(s"${routerId}_$workerID", id, trackedTimeToUse, m)
      case m: EdgeDelete =>
        TrackedEdgeDelete(s"${routerId}_$workerID", id, trackedTimeToUse, m)
    }
    log.debug(s"RouterWorker sending message [$sentMessage] to PubSub")
    if (trackedMessage)
      mediator ! DistributedPubSubMediator
        .Send("/user/WatermarkManager", UpdateArrivalTime(trackedTime, message.msgTime), localAffinity = false)

    mediator ! DistributedPubSubMediator.Send(path, sentMessage, localAffinity = false)
    message.msgTime
  }

  private def getMessageIDForWriter(path: String) =
    messageIDs.get(path) match {
      case Some(messageId) =>
        messageIDs put (path, messageId + 1)
        messageId
      case None =>
        messageIDs put (path, 1)
        0
    }
}

object RouterWorker {
  object CommonMessage {
    case object TimeBroadcast
  }
}
