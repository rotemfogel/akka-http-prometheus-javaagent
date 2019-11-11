package me.rotemfo.common.api.routes

import akka.http.scaladsl.model.{IllegalUriException, StatusCodes}
import akka.http.scaladsl.server.{Directives, ExceptionHandler, Route}
import me.rotemfo.common.api.model.Status
import me.rotemfo.common.logging.Logging

/**
 * project: scala-demo-server
 * package: me.rotemfo.common.api.routes
 * file:    BaseRoute
 * created: 2019-11-11
 * author:  rotem
 */
trait BaseRoute extends Directives
  with Json4sSupport with MeterDirectives
  with TimerDirectives with StatusCodeCounterDirectives
  with Logging {

  import ErrorMessages.{corsError, generalError}

  implicit private def defaultExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case _: IllegalUriException =>
        complete((StatusCodes.NotFound, Status(StatusCodes.NotFound.intValue, Some("CORS error"), corsError)))
      case e: Throwable =>
        logger.error(generalError, e)
        complete((StatusCodes.InternalServerError, Status(StatusCodes.InternalServerError.intValue, generalError)))
    }

  def routes: Route

  protected def base(dsl: Route): Route = {
    val route: Route =
      extractRequest { request =>
        withMeter {
          withTimer {
            withStatusCodeCounter {
              pathPrefix("api" / "v1") {
                dsl
              }
            }
          }
        }
      }
    handleExceptions(defaultExceptionHandler) {
      route
    }
  }
}
