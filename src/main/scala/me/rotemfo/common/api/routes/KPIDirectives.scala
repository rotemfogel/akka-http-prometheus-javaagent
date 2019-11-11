package me.rotemfo.common.api.routes

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server._
import me.rotemfo.common.counters.PerfCounters

/**
 * project: scala-demo-server
 * package: me.rotemfo.common.api.routes
 * file:    KPIDirectives
 * created: 2019-11-11
 * author:  rotem
 */
object KPIDirectives {
  final val GLOBAL = "global"
  final val HTTP = "HTTP"
  final val TIMER = "t"
  final val METER = "m"
  final val STATUS = "c"

  def getKPIName(ctx: RequestContext, kpiType: String): String = {
    val methodName = ctx.request.method.name.toLowerCase
    getKPIName(ctx.request.uri, kpiType, methodName)
  }

  def getKPIName(uri: Uri, kpiType: String, methodName: String): String = {
    val routeName = {
      val paths = uri.path.toString.drop(1).split("/")
      val path = {
        if (paths.length == 1) paths.take(1)
        else paths.take(3)
      }
      path.mkString(".")
    }
    s"$kpiType.$routeName.$methodName"
  }
}

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Directive0, MalformedRequestContentRejection, MethodRejection, RequestContext, RouteResult, TransformationRejection}
import com.codahale.metrics.{Counter, Meter, Timer}
import KPIDirectives._

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

trait KPIDirectives extends BasicDirectives {
  def mark(name: String, group: String = HTTP): Meter = PerfCounters.mark(name, group)
  def time(name: String, group: String = HTTP): Timer.Context = PerfCounters.getTimer(name, group).time()
  def increment(name: String, group: String = HTTP): Counter = PerfCounters.increment(name, group)
}

trait TimerDirectives extends KPIDirectives {
  def withTimer: Directive0 =
    timer(ctx => getKPIName(ctx, TIMER))

  def withNamedTimer(name: String): Directive0 =
    timer(_ => name)

  private[routes] def timer(nameFn: RequestContext => String): Directive0 = {
    extractExecutionContext.flatMap {
      implicit executionContext: ExecutionContext =>
        mapInnerRoute { inner =>
          ctx =>
            val timer = time(nameFn(ctx))
            try {
              inner(ctx)
            } catch {
              case NonFatal(err) => ctx.fail(err)
            } finally {
              timer.stop()
            }
        }
    }
  }
}

trait MeterDirectives extends KPIDirectives {
  def withMeter: Directive0 =
    meter(ctx => getKPIName(ctx, METER))

  //noinspection AutoTupling
  private[routes] def meter(nameFn: RequestContext => String): Directive0 = {
    extractExecutionContext.flatMap {
      implicit executionContext: ExecutionContext =>
        mapInnerRoute { inner =>
          ctx =>
            try {
              val fut = inner(ctx)
              executionContext.execute(() => {
                mark(nameFn(ctx))
              })
              fut
            } catch {
              case NonFatal(err) => ctx.fail(err)
            }
        }
    }
  }

  def withNamedMeter(name: String): Directive0 =
    meter(_ => name)
}

trait StatusCodeCounterDirectives extends KPIDirectives {

  def withStatusCodeCounter: Directive0 =
    responseCodes(ctx => getKPIName(ctx, STATUS))

  def withNamedStatusCodeCounter(name: String): Directive0 =
    responseCodes((_: RequestContext) => name)

  import ErrorMessages._

  //noinspection AutoTupling
  private[routes] def responseCodes(nameFn: RequestContext => String): Directive0 = {
    extractExecutionContext.flatMap {
      implicit executionContext: ExecutionContext =>
        mapInnerRoute { inner =>
          ctx =>
            try {
              val fut = inner(ctx)
              val name = nameFn(ctx)
              fut foreach {
                case RouteResult.Complete(resp) =>
                  executionContext.execute(() => {
                    val nameAndCode = s"$name.${liftStatusCode(resp.status)}"
                    mark(s"$name._")
                    mark(s"$nameAndCode._")
                    mark(s"$GLOBAL.${resp.status.intValue}._")
                  })
                case RouteResult.Rejected(rejections) =>
                  rejections.foreach {
                    case _: AuthenticationFailedRejection =>
                      mark(s"$name.$authenticationFailedRejection")
                      mark(s"$GLOBAL.$authenticationFailedRejection")
                      mark(s"$name.4xx._")
                      mark(s"$GLOBAL.4xx._")
                    case _: MalformedRequestContentRejection =>
                      mark(s"$name.$malformedRequestContentRejection")
                      mark(s"$GLOBAL.$malformedRequestContentRejection")
                    case _: TransformationRejection =>
                    case _: MethodRejection =>
                    case _ =>
                      mark(s"$name.$rejection")
                      mark(s"$GLOBAL.$rejection")
                  }
              }
              fut
            } catch {
              case NonFatal(err) =>
                executionContext.execute(() => {
                  mark(s"${nameFn(ctx)}.${StatusCodes.InternalServerError.intValue}._")
                  mark(s"$GLOBAL.${StatusCodes.InternalServerError.intValue}._")
                })
                ctx.fail(err)
            }
        }
    }
  }

  private[routes] def liftStatusCode(code: StatusCode): String =
    code match {
      case _: StatusCodes.Informational => "1xx"
      case _: StatusCodes.Success => "2xx"
      case _: StatusCodes.Redirection => "3xx"
      case _: StatusCodes.ClientError => "4xx"
      case _: StatusCodes.ServerError => "5xx"
      case StatusCodes.CustomStatusCode(custom) => custom.toString
    }
}
