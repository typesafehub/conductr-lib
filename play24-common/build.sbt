name := "play24-conductr-lib-common"

libraryDependencies ++= List(
  Library.play24Ws % "provided"
)

crossScalaVersions := crossScalaVersions.value.filterNot(_.startsWith("2.12"))
