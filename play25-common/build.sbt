name := "play25-conductr-lib-common"

libraryDependencies ++= List(
  Library.play25Ws % "provided"
)

crossScalaVersions := crossScalaVersions.value.filterNot(_.startsWith("2.12"))
