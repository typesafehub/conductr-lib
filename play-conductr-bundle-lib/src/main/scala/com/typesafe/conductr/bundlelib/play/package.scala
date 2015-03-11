/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib

import _root_.play.libs.F

import _root_.scala.concurrent.Future

package object play {

  /**
   * Conveniently convert from Scala Option => Play's F.Option
   */
  implicit class OptionOps[T](underlying: Option[T]) {
    def toF: F.Option[T] = underlying match {
      case Some(x) => new F.Some(x)
      case None    => new F.None()
    }
  }

  /**
   * Conveniently convert from Scala Future => Play's F.Promise
   */
  implicit class FutureOps[T](underlying: Future[T]) {
    def toF: F.Promise[T] =
      F.Promise.wrap(underlying)
  }
}
