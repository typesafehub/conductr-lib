/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

name := "test-lib"

libraryDependencies ++= List(
  Library.akka23Testkit,
  Library.akka23HttpTestkit,
  Library.junit,
  Library.scalaTest
)
