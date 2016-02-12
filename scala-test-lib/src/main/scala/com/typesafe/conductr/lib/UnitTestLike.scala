package com.typesafe.conductr.lib

import org.scalatest.{ Matchers, WordSpec, WordSpecLike }

/**
 * Unit tests for Typesafe ConductR.
 */
trait UnitTestLike extends WordSpecLike with Matchers

/**
 * Unit tests for Typesafe ConductR as an abstract class (improves compilation speed).
 */
abstract class UnitTest extends WordSpec with Matchers
