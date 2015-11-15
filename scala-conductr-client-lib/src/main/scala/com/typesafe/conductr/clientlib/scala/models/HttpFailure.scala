package com.typesafe.conductr.clientlib.scala.models

/**
 * Defines the members of an http failure class
 */
trait HttpFailure {
  def code: Int
  def error: String
}