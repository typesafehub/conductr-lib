name := "play23-conductr-client-lib"

crossScalaVersions := crossScalaVersions.value.filterNot(_.startsWith("2.12"))
scalacOptions += "-target:jvm-1.6"