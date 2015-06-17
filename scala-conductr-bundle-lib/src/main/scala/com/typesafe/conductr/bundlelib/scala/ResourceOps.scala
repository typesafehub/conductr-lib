package com.typesafe.conductr.bundlelib.scala

import java.net.{ URI => JavaURI, URL => JavaURL }

/**
 * Conveniently build a URI
 */
object URI {
  def apply(s: String): JavaURI =
    new JavaURI(s)
}

/**
 * Conveniently build a URL
 */
object URL {
  def apply(s: String): JavaURL =
    new JavaURL(s)
}
