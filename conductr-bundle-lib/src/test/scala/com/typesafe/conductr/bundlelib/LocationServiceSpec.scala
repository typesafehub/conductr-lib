/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib

import com.typesafe.conductr.UnitTest

class LocationServiceSpec extends UnitTest {

  "The LocationService functionality in the library" should {
    "return null when running in development mode" in {
      LocationService.createLookupPayload("/whatever") shouldBe null
    }
  }
}
