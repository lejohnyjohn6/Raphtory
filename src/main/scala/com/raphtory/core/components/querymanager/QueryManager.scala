package com.raphtory.core.components.querymanager

import akka.actor.{ActorLogging, ActorRef, Props, Stash}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import com.raphtory.core.components.RaphtoryActor
import com.raphtory.core.components.analysismanager.AnalysisRestApi.message.AnalysisRequest
import com.raphtory.core.components.orchestration.raphtoryleader.WatchDog.Message.{ClusterStatusRequest, ClusterStatusResponse, QueryManagerUp}
import com.raphtory.core.components.querymanager.QueryManager.Message.{EndQuery, LiveQuery, ManagingTask, PointQuery, QueryNotPresent, RangeQuery, StartUp}
import com.raphtory.core.components.querymanager.QueryManager.State
import com.raphtory.core.components.querymanager.handler.{LiveQueryHandler, PointQueryHandler, RangeQueryHandler}
import com.raphtory.core.model.algorithm.GraphAlgorithm

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, SECONDS}
import scala.tools.scalap.scalax.rules.scalasig.ScalaSigEntryParsers.ref

class QueryManager extends RaphtoryActor with ActorLogging with Stash {

  implicit val executionContext: ExecutionContext = context.system.dispatcher

  final protected val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Put(self)

  override def preStart() {
    context.system.scheduler.scheduleOnce(Duration(5, SECONDS), self, StartUp)
  }

  override def receive: Receive = init()

  def init(): Receive = {
    case StartUp =>
      mediator ! new DistributedPubSubMediator.Send("/user/WatchDog", QueryManagerUp(0))
      mediator ! new DistributedPubSubMediator.Send("/user/WatchDog", ClusterStatusRequest) //ask if the cluster is safe to use

    case ClusterStatusResponse(clusterUp) =>
      if (clusterUp) context.become(work(State(Map.empty)))
      else context.system.scheduler.scheduleOnce(Duration(1, SECONDS), self, StartUp)

    case _: AnalysisRequest =>
      stash()

    case unhandled =>
      log.error(s"unexpected message $unhandled during init stage")
  }

  def work(state: State): Receive = {
    case StartUp => // do nothing as it is ready

    case query: PointQuery =>
      val jobID = getID(query.algorithm)
      val queryHandler = spawnPointQuery(jobID,query)
      trackNewQuery(state,jobID, queryHandler)

    case query: RangeQuery =>
      val jobID = getID(query.algorithm)
      val queryHandler = spawnRangeQuery(jobID,query)
      trackNewQuery(state,jobID, queryHandler)

    case query: LiveQuery =>
      val jobID = getID(query.algorithm)
      val queryHandler = spawnLiveQuery(jobID,query)
      trackNewQuery(state,jobID, queryHandler)

    case req: EndQuery =>
      state.currentQueries.get(req.jobID) match {
        case Some(actor) =>
          context.become(work(state.updateCurrentTask(_ - req.jobID)))
          actor forward EndQuery
        case None => sender ! QueryNotPresent(req.jobID)
      }

  }

  private def spawnPointQuery(id:String, query: PointQuery): ActorRef = {
    log.info(s"Point Query received, your job ID is $id")
    context.system.actorOf(Props(PointQueryHandler(id,query.algorithm,query.timestamp,query.windows)).withDispatcher("analysis-dispatcher"), id)
  }

  private def spawnRangeQuery(id:String, query: RangeQuery): ActorRef = {
    log.info(s"Range Query received, your job ID is $id")
    context.system.actorOf(Props(RangeQueryHandler(id,query.algorithm,query.start,query.end,query.increment,query.windows)).withDispatcher("analysis-dispatcher"), id)
  }

  private def spawnLiveQuery(id:String, query: LiveQuery): ActorRef = {
    log.info(s"Range Query received, your job ID is $id")
    context.system.actorOf(Props(LiveQueryHandler(id,query.algorithm,query.increment,query.windows)).withDispatcher("analysis-dispatcher"), id)
  }


  private def getID(algorithm:GraphAlgorithm):String = {
    algorithm.getClass + "_" + System.currentTimeMillis()
  }

  private def trackNewQuery(state:State,jobID:String,queryHandler:ActorRef):Unit = {
    sender() ! ManagingTask(queryHandler)
    context.become(work(state.updateCurrentTask(_ ++ Map((jobID,queryHandler)))))
  }

}




object QueryManager {
  private case class State(currentQueries: Map[String, ActorRef]) {
    def updateCurrentTask(f: Map[String, ActorRef] => Map[String, ActorRef]): State =
      copy(currentQueries = f(currentQueries))
  }
  object Message {
    case class PointQuery(algorithm:GraphAlgorithm, timestamp: Long, windows: List[Long])
    case class RangeQuery(algorithm:GraphAlgorithm, start: Long, end: Long, increment: Long, windows: List[Long])
    case class LiveQuery(algorithm:GraphAlgorithm, increment: Long, windows: List[Long])
    case class EndQuery(jobID:String)
    case class QueryNotPresent(jobID:String)

    case class  RequestResults(jobId: String)
    case class  KillTask(jobId: String)
    case class  ResultsForApiPI(results: Array[String])
    case class  ManagingTask(actor:ActorRef)
    case class  TaskFinished(result:Boolean)
    case object StartUp
    case object JobKilled
    case object JobDoesntExist
    case object AreYouFinished
    case object JobFailed
  }
}
