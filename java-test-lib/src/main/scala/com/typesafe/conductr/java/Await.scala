package com.typesafe.conductr.java

import java.util.concurrent.TimeUnit
import java.util.concurrent.CompletionStage

import scala.concurrent.duration.FiniteDuration

object Await {
  def result[T](s: CompletionStage[T], finiteDuration: FiniteDuration): T =
    s.toCompletableFuture.get(finiteDuration.toMillis, TimeUnit.MILLISECONDS)
}
