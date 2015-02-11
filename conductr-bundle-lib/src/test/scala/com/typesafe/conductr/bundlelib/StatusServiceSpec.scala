/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib

import com.typesafe.conductr.UnitTest

class StatusServiceSpec extends UnitTest {

  "The StatusService functionality in the library" should {

    "not fail when running in development mode" in {
      StatusService.createSignalStartedPayload() shouldBe null
    }
  }
}
