package me.rotemfo.server

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Random

/**
 * project: akka-http-prometheus-javaagent
 * package: me.rotemfo.server
 * file:    Client
 * created: 2019-11-13
 * author:  rotem
 */
class Client extends Actor with ActorLogging {
  implicit private val actorSystem: ActorSystem = context.system
  implicit private val executionContext: ExecutionContext = actorSystem.dispatcher
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  private lazy val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] = Http().outgoingConnection("localhost", 8080)

  private def httpSingleRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(connectionFlow).runWith(Sink.head)

  private def executeHttpRequest(httpRequest: HttpRequest): Unit = {
    val responseFuture: Future[HttpResponse] = httpSingleRequest(httpRequest)
    var response: HttpResponse = null
    try {
      response = Await.result[HttpResponse](responseFuture, 1.seconds)
      log.info(s"${response.status.intValue()}")
    } catch {
      case _: java.util.concurrent.TimeoutException => log.error("timeout error")
      case t: Throwable => log.error(s"error executing http request", t)
    }
  }

  private final val request = HttpRequest(uri = "/api/v1/users")

  private def sleep(millis: Int): Unit = {
    try {
      Thread.sleep(millis)
    } catch {
      case _: InterruptedException =>
    }
  }

  override def receive: Receive = {
    case _ =>
      // wait 2 seconds before sending the first call
      sleep(3000)
      log.info("starting client")
      while (true) {
        try {
          executeHttpRequest(request)
          sleep(Random.self.nextInt(100) + 1)
        } catch {
          case e: Throwable =>
            log.error("error executing http request", e)
        }
      }
  }

  self ! "start"
}

object Client {
  def props: Props = Props[Client]
}