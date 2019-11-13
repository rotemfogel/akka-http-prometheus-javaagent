package me.rotemfo.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import me.rotemfo.common.api.routes.UsersRoute
import me.rotemfo.common.logging.Logging

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

/**
 * project: scala-demo-server
 * package: me.rotemfo.server
 * file:    Server
 * created: 2019-11-11
 * author:  rotem
 */
object Server extends App with Logging {
  implicit val actorSystem: ActorSystem = ActorSystem("demo-server")
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private final val host: String = "0.0.0.0"
  private final val port: Int = 8080

  val bindingFuture = Http().bindAndHandle(UsersRoute.routes, host, port)
  logger.info(s"server is up and running at: $host:$port")
  logger.info(s"serving: http://$host:$port/api/v1/users")
  val client = actorSystem.actorOf(Client.props)
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => actorSystem.terminate()) // and shutdown when done
}