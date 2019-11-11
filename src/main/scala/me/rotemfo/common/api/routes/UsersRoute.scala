package me.rotemfo.common.api.routes

import java.util.concurrent.atomic.AtomicLong

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import me.rotemfo.common.api.model.Status

/**
 * project: scala-demo-server
 * package: me.rotemfo.common.api.routes
 * file:    UsersRoute
 * created: 2019-11-11
 * author:  rotem
 */
object UsersRoute extends BaseRoute {
  private final val counter: AtomicLong = new AtomicLong(0)

  import StatusCodes._

  override def routes: Route = {
    base(pathPrefix("users") {
      pathEnd {
        get {
          val c = counter.incrementAndGet()
          val response = c % 5 match {
            case 0 => Status(OK.intValue, OK.defaultMessage)
            case 1 => Status(Created.intValue, Created.defaultMessage)
            case 2 => Status(PermanentRedirect.intValue, PermanentRedirect.defaultMessage)
            case 3 => Status(BadRequest.intValue, BadRequest.defaultMessage)
            case 4 => Status(InternalServerError.intValue, InternalServerError.defaultMessage)
          }
          complete((response.code, response))
        }
      }
    })
  }
}
