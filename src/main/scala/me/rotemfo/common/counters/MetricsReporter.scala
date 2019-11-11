package me.rotemfo.common.counters

import java.time.Instant
import java.util.concurrent.TimeUnit

import com.codahale.metrics._
import me.rotemfo.common.logging.Logging

import scala.collection.JavaConverters._

/**
 * project: scala-demo-server
 * package: me.rotemfo.common.counters
 * file:    MetricsReporter
 * created: 2019-11-11
 * author:  rotem
 */
class MetricsReporter(registry: MetricRegistry)
  extends ScheduledReporter(registry, "metrics", MetricFilter.ALL, TimeUnit.MILLISECONDS, TimeUnit.MILLISECONDS) with Logging {

  override def report(gauges: java.util.SortedMap[String, Gauge[_]],
                      counters: java.util.SortedMap[String, Counter],
                      histograms: java.util.SortedMap[String, Histogram],
                      meters: java.util.SortedMap[String, Meter],
                      timers: java.util.SortedMap[String, Timer]): Unit = {
    val countersSet = counters.entrySet.asScala
    PerfCounters.mark(measureName = "MetricsReporter_counters", value = countersSet.size)
    countersSet.foreach(entry => {
      val key = entry.getKey
      val value = entry.getValue
      JMX.registerMBean(value, key)
      JMXCounters.update(key, value)
    })

    val metersSet = meters.entrySet.asScala
    PerfCounters.mark(measureName = "MetricsReporter_meters", value = metersSet.size)
    metersSet.foreach(entry => {
      val key = entry.getKey
      val value = entry.getValue
      JMX.registerMBean(value, key)
      JMXMeters.update(key, value)
    })

    val timersSet = timers.entrySet.asScala
    PerfCounters.mark(measureName = "MetricsReporter_timers", value = timersSet.size)
    timersSet.foreach(entry => {
      val key = entry.getKey
      val value = entry.getValue
      JMX.registerMBean(value, key)
      JMXTimers.update(key, value)
    })
  }

  def getMetricAsMeasure(name: String, metric: Metric, convertDuration: Double => Double, convertRate: Double => Double): Measure = {
    val measure = metric match {
      case gauge: Gauge[_] => getGaugeAsMeasure(name, gauge)
      case counter: Counter => getCounterAsMeasure(name, counter)
      case timer: Timer => getTimerAsMeasure(name, timer, convertDuration, convertRate)
      case meter: Meter => getMeterAsMeasure(name, meter, convertRate)
    }
    measure
  }

  private def getTimerAsMeasure(name: String, timer: Timer, convertDuration: Double => Double, convertRate: Double => Double): Measure = {
    val snapshot = timer.getSnapshot
    Measure("TIMER", name, timer.getCount, Some(convertDuration(snapshot.getMin)), Some(convertDuration(snapshot.getMax)),
      Some(convertDuration(snapshot.getMean)), Some(convertRate(timer.getMeanRate)))
  }

  def getMeterAsMeasure(name: String, meter: Meter, convertRate: Double => Double): Measure = {
    Measure("METER", name, meter.getCount, None, None, None, Some(convertRate(meter.getMeanRate)))
  }

  def getCounterAsMeasure(name: String, counter: Counter): Measure = {
    Measure("COUNTER", name, counter.getCount)
  }

  def getGaugeAsMeasure(name: String, gauge: Gauge[_]): Measure = {
    Measure("GAUGE", name, gauge.getValue.asInstanceOf[Long])
  }
}

case class Measure(measure_type: String, measure_name: String, measure_count: Long,
                   measure_min: Option[Double] = None, measure_max: Option[Double] = None,
                   measure_mean: Option[Double] = None, measure_meanRate: Option[Double] = None,
                   timestamp: Long = Instant.now.toEpochMilli)

