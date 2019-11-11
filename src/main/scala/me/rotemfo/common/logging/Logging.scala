package me.rotemfo.common.logging

import org.slf4j.{Logger, LoggerFactory}

/**
 * project: scala-demo-server
 * package: me.rotemfo.common.logging
 * file:    Logging
 * created: 2019-11-11
 * author:  rotem
 */
trait Logging {
  protected final val logger: Logger = LoggerFactory.getLogger(getClass)
}
