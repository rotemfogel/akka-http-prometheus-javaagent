package me.rotemfo.common.api.model

/**
 * project: scala-demo-server
 * package: me.rotemfo.common.api.model
 * file:    Status
 * created: 2019-11-11
 * author:  rotem
 */
case class Status(code: Int, service: Option[String], message: String) {
  @inline override def equals(obj: scala.Any): Boolean = {
    obj match {
      case other: Status =>
        code.equals(other.code) &&
          service.equals(other.service) &&
          message.equals(other.message)
      case _ => false
    }
  }
}

object Status {
  def apply(message: String): Status = Status(200, None, message)

  def apply(code: Int, message: String): Status = Status(code, None, message)

  def apply(code: Int): Status = Status(code, None, "")
}

