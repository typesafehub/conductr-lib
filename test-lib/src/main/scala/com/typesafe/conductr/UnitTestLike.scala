/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr

import org.scalatest.{ Matchers, WordSpec, WordSpecLike }

/**
 * Unit tests for Typesafe ConductR.
 */
trait UnitTestLike extends WordSpecLike with Matchers

/**
 * Unit tests for Typesafe ConductR as an abstract class (improves compilation speed).
 */
abstract class UnitTest extends WordSpec with Matchers
