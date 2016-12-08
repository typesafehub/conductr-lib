name := "akka23-conductr-lib-common"

libraryDependencies ++= List(
  Library.akka23Http
)

crossScalaVersions := crossScalaVersions.value.filterNot(_.startsWith("2.12"))
