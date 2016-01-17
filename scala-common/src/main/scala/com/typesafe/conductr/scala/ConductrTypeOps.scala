package com.typesafe.conductr.scala

object ConductrTypeOps {

  /**
   * Extension methods for `scala.Array[Byte]`.
   * @param bytes the bytes to be extended
   */
  implicit class ByteArrayOps(bytes: Array[Byte]) {

    /**
     * Convert an `scala.Array[Byte]` to a `java.lang.String` in hexadecimal format.
     * Positive bytes are mapped to 00..7f, negative ones 80..ff.
     * @return `java.lang.String` in hexadecimal format
     */
    def toHex: String =
      bytes.map(byte => f"$byte%02x").mkString("")
  }

  /**
   * Convert a `java.lang.String` which is required to be in hexadecimal format to an `scala.Array[Byte]`.
   * 00..7f is mapped to positive bytes 80..ff to negative ones; non-hex characters are ignored.
   * @return `scala.Array[Byte]`
   */
  def hexStringToByteArray(hex: String): Array[Byte] =
    hex.replaceAll("[^0-9A-Fa-f]", "").sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
}
