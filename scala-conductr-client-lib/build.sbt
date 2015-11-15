name := "scala-conductr-client-lib"

libraryDependencies ++= List(
  Library.akka23HttpTestkit % "test",
  Library.akka23Testkit     % "test",
  Library.scalaTest         % "test"
)

fork in Test := true
