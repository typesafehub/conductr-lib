/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.akka

import com.typesafe.conductr.AkkaUnitTest

class ClusterPropertiesSpecWithEnvForHost extends AkkaUnitTest("ClusterPropertiesSpecWithEnvForHost", "akka.loglevel = INFO") {

  ClusterProperties.initialize()

  "The ClusterProperties functionality in the library" should {
    "return seed properties when running with no other seed nodes" in {
      sys.props.get("akka.cluster.seed-nodes.0") shouldBe Some("akka.tcp://some-system@10.0.1.10:10000")
    }
  }
}
