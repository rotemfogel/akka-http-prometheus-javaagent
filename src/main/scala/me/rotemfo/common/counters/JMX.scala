package me.rotemfo.common.counters

import java.lang.management.ManagementFactory
import java.util.concurrent.locks.{ReadWriteLock, ReentrantReadWriteLock}

import com.codahale.metrics.{Counter, Meter, Timer}
import javax.management.{MBeanServer, ObjectName}
import me.rotemfo.common.logging.Logging

import scala.collection.mutable

/**
  * project: scala-demo-server
  * package: me.rotemfo.common.counters
  * file:    Model
  * created: 2018-09-02
  * author:  rotem
  */
object JMX extends Logging {
  private val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer
  private var registeredMBeans: Set[String] = Set()
  private val lock: ReadWriteLock = new ReentrantReadWriteLock()

  def exists(name: String): Boolean = registeredMBeans.contains(name)

  def parseArgs(name: String): (MeasureArgs, String) = {
    val args = MeasureArgs.fromString(name)
    (args, s"${args.group}:${args.measureName}")
  }

  def registerMBean(perf: AnyRef, name: String): Unit = {
    val (args, argName) = parseArgs(name)
    lock.readLock().lock()
    val exists = try {
      registeredMBeans(name)
    } finally {
      lock.readLock().unlock()
    }
    if (!exists) {
      lock.writeLock().lock()
      val classType: String = perf.getClass.getSimpleName
      try {
        val mbean = classType match {
          case c if c.equals("Counter") => JMXCounters.register(argName, perf.asInstanceOf[Counter])
          case t if t.equals("Timer") => JMXTimers.register(argName, perf.asInstanceOf[Timer])
          case m if m.equals("Meter") => JMXMeters.register(argName, perf.asInstanceOf[Meter])
        }
        mbs.registerMBean(mbean, new ObjectName(s"me.rotemfo.counters.${classType.toLowerCase}:type=${args.group},name=${args.measureName}"))
        registeredMBeans = registeredMBeans + argName
      } catch {
        case _: javax.management.InstanceAlreadyExistsException =>
        case t: Throwable => logger.error(s"error registering JMX property with name $argName", t)
      } finally {
        lock.writeLock().unlock()
      }
    }
  }
}

trait ICounter {
  def getCount: Long
}

trait IMeter extends ICounter {
  def getFifteenMinuteRate: Double
  def getFiveMinuteRate: Double
  def getOneMinuteRate: Double
  def getMeanRate: Double
}

trait ITimer extends IMeter {
  def getMedian: Double
  def getMax: Long
  def getMin: Long
  def getStdDev: Double
}

trait JMXCounterMBean extends ICounter

trait JMXMeterMBean extends IMeter

trait JMXTimerMBean extends ITimer

case class JMXCounter(var count: Long) extends JMXCounterMBean {
  override def getCount: Long = count
}

object JMXCounters {
  private val counters = mutable.Map[String, JMXCounter]()

  def register(name: String, counter: Counter): JMXCounter = {
    val c = counters.get(name)
    val o = if (c.isDefined) {
      c.get.count = counter.getCount
      c.get
    } else JMXCounter(counter.getCount)
    counters(name) = o
    o
  }

  def update(name: String, counter: Counter): JMXCounter = {
    register(JMX.parseArgs(name)._2, counter)
  }
}

case class JMXMeter(var m1Rate: Double, var m5Rate: Double, var m15Rate: Double, var count: Long, var mean: Double) extends JMXMeterMBean {
  override def getCount: Long = count
  override def getFifteenMinuteRate: Double = m15Rate
  override def getFiveMinuteRate: Double = m5Rate
  override def getOneMinuteRate: Double = m1Rate
  override def getMeanRate: Double = mean
}

object JMXMeters {
  private val meters = mutable.Map[String, JMXMeter]()

  def register(name: String, meter: Meter): JMXMeter = {
    val m = meters.get(name)
    val o = if (m.isDefined) {
      m.get.count = meter.getCount
      m.get.m1Rate = meter.getOneMinuteRate
      m.get.m5Rate = meter.getFifteenMinuteRate
      m.get.m15Rate = meter.getFifteenMinuteRate
      m.get.mean = meter.getMeanRate
      m.get
    } else JMXMeter(meter.getOneMinuteRate, meter.getFiveMinuteRate, meter.getFifteenMinuteRate, meter.getCount, meter.getMeanRate)
    meters(name) = o
    o
  }

  def update(name: String, meter: Meter): JMXMeter = {
    register(JMX.parseArgs(name)._2, meter)
  }
}

case class JMXTimer(var m1Rate: Double, var m5Rate: Double, var m15Rate: Double, var count: Long, var mean: Double,
                    var median: Double, var max: Long, var min: Long, var stddev: Double) extends JMXTimerMBean {
  override def getCount: Long = count
  override def getFifteenMinuteRate: Double = m15Rate
  override def getFiveMinuteRate: Double = m5Rate
  override def getOneMinuteRate: Double = m1Rate
  override def getMeanRate: Double = mean
  override def getMedian: Double = median
  override def getMax: Long = max
  override def getMin: Long = min
  override def getStdDev: Double = stddev
}

object JMXTimers {
  private val timers = mutable.Map[String, JMXTimer]()

  def register(name: String, timer: Timer): JMXTimer = {
    val snap = timer.getSnapshot
    val m = timers.get(name)
    val o = if (m.isDefined) {
      m.get.count = timer.getCount
      m.get.m1Rate = timer.getOneMinuteRate
      m.get.m5Rate = timer.getFifteenMinuteRate
      m.get.m15Rate = timer.getFifteenMinuteRate
      m.get.mean = snap.getMean
      m.get.max = snap.getMax
      m.get.min = snap.getMin
      m.get.median = snap.getMedian
      m.get.stddev = snap.getStdDev
      m.get
    } else JMXTimer(timer.getOneMinuteRate, timer.getFiveMinuteRate, timer.getFifteenMinuteRate, timer.getCount,
      snap.getMean, snap.getMedian, snap.getMax, snap.getMin, snap.getStdDev)
    timers(name) = o
    o
  }

  def update(name: String, timer: Timer): JMXTimer = {
    register(JMX.parseArgs(name)._2, timer)
  }
}