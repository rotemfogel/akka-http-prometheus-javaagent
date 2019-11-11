package me.rotemfo.common.api.routes

/**
 * project: scala-demo-server
 * package: me.rotemfo.common.api.routes
 * file:    ErrorMessages
 * created: 2019-11-11
 * author:  rotem
 */
object ErrorMessages {
  final val authenticationFailedRejection: String = "authentication failed rejection"
  final val corsError: String = "Domain is not allowed access to this service"
  final val malformedRequestContentRejection: String = "malformed request content rejection"
  final val rejection: String = "rejection"
  final val generalError: String = "An unrecoverable error has occurred in Http Server"
}
