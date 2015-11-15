package com.typesafe.conductr.clientlib

import java.util.zip.ZipInputStream

package object scala {

  /**
   * Something that can be closed, e.g. a `java.io.InputStream` or a door.
   */
  type Closeable = { def close(): Unit }

  /**
   * Execute around pattern to close a [[Closeable]]
   * after applying a function to it.
   * @param a the [[Closeable]], i.e. something that can be closed
   * @param f the function to be applied to the [[Closeable]]
   */
  def withCloseable[A <: Closeable, B](a: A)(f: A => B): B =
    try
      f(a)
    finally
      a.close()

  def withZipInputStream[T](in: ZipInputStream)(f: ZipInputStream => T): T =
    try {
      f(in)
    } finally {
      in.closeEntry()
      in.close()
    }
}
