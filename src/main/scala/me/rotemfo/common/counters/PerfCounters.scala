package me.rotemfo.common.counters

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import com.codahale.metrics._
import me.rotemfo.common.logging.Logging

/**
 * project: scala-demo-server
 * package: me.rotemfo.common.counters
 * file:    PerfCounters
 * created: 2019-11-11
 * author:  rotem
 */
object PerfCounters extends Logging {
  private lazy val metrics = new MetricRegistry
  val root = "ROOT"

  private val reporter = new MetricsReporter(metrics)
  reporter.start(2, TimeUnit.SECONDS)

  def getCount(measureName: String, group: String = root): Long = {
    val name = MeasureArgs.asString(measureName, group)
    metrics.counter(name).getCount
  }

  def getMeter(measureName: String, group: String = root): Meter = {
    val name = MeasureArgs.asString(measureName, group)
    val meter = metrics.meter(name)
    meter
  }

  def getTimer(measureName: String, group: String = root): Timer = {
    val name = MeasureArgs.asString(measureName, group)
    val timer = metrics.timer(name)
    timer
  }

  def increment(measureName: String, group: String = root, value: Long = 1L): Counter = {
    val name = MeasureArgs.asString(measureName, group)
    val counter = metrics.counter(name)
    if (value > 0) counter.inc(value)
    counter
  }

  def decrement(measureName: String, group: String = root, value: Long = 1L): Counter = {
    val name = MeasureArgs.asString(measureName, group)
    val counter = metrics.counter(name)
    if (value > 0) counter.dec(value)
    counter
  }

  def mark(measureName: String, group: String = root, value: Long = 1L): Meter = {
    val name = MeasureArgs.asString(measureName, group)
    val meter = metrics.meter(name)
    if (value > 0) meter.mark(value)
    meter
  }

  /**
   * Replace all special characters except following: -._
   */
  def escapeJmx(name: String): String = {
    name.replaceAll("[^a-zA-Z0-9\\.\\-_]", "")
  }
}

case class MeasureArgs(measureName: String, group: String)

object MeasureArgs {
  private val mapGroup: ConcurrentHashMap[String, String] = new ConcurrentHashMap
  private var mapGroupFunction: Long => String = (group: Long) => group.toString
  private val cacheEscapedNames: ConcurrentHashMap[String, String] = new ConcurrentHashMap

  def init(mapFunction: Long => String): Unit = {
    mapGroupFunction = mapFunction
  }

  // This code can be removed when moving analytics to 2.12

  import java.util.function.{Function => JFunction}

  //noinspection ConvertExpressionToSAM
  def toJavaFunction[A, B](f: Function[A, B]): JFunction[A, B] = new JFunction[A, B] {
    override def apply(a: A): B = f(a)
  }

  def asString(measureName: String, group: String): String = {
    val escapedEnrichedGroupName = getEscapedEnrichedGroup(group)
    val escapedMeasureName = cacheEscapedNames.computeIfAbsent(measureName, toJavaFunction(measureName => {
      val escapedName = PerfCounters.escapeJmx(measureName)
      escapedName
    }))

    s"$escapedMeasureName#$escapedEnrichedGroupName#END"
  }

  def fromString(args: String): MeasureArgs = {
    val spl = args.split("#")
    MeasureArgs(spl(0), spl(1))
  }

  private def getEscapedEnrichedGroup(group: String): String = {
    mapGroup.computeIfAbsent(group, toJavaFunction(group => {
      val id = toLongOpt(group)
      val enrichedGroup = if (id.isDefined) mapGroupFunction(id.get) else group
      val escapedEnrichedGroupName = PerfCounters.escapeJmx(enrichedGroup)
      escapedEnrichedGroupName
    }))
  }

  private def toLongOpt(group: String): Option[Long] = {
    try {
      Some(group.toLong)
    } catch {
      case _: Throwable => None
    }
  }
}
