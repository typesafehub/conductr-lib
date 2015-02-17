/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.akka

import com.typesafe.conductr.AkkaUnitTest

class ClusterPropertiesSpecWithEnvForOthers extends AkkaUnitTest("ClusterPropertiesSpecWithEnvForOthers", "akka.loglevel = INFO") {

  ClusterProperties.initialize()

  "The ClusterProperties functionality in the library" should {
    "return seed properties when running with other seed nodes" in {
      sys.props.get("akka.cluster.seed-nodes.0") shouldBe Some("akka.udp://some-system@10.0.1.11:10001")
      sys.props.get("akka.cluster.seed-nodes.1") shouldBe Some("akka.tcp://some-system@10.0.1.12:10000")
      sys.props.get("akka.cluster.seed-nodes.2") shouldBe None
    }
  }
}
