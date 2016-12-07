name := "java-test-lib"

scalacOptions += "-target:jvm-1.8"
crossScalaVersions := List(crossScalaVersions.value.head) // Requires building just the once
