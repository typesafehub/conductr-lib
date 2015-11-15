package com.typesafe.conductr.clientlib.scala

/**
 * Valid HTTP status codes used by the ConductR control server.
 */
private object HttpStatusCodes {
  final val Ok = 200
  final val MultipleChoices = 300
  final val BadRequest = 400
  final val NotFound = 404

  def isSuccess(code: Int): Boolean =
    code >= 200 && code <= 299
}
