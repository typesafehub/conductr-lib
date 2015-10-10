name := "scala-test-lib"

libraryDependencies ++= List(
  Library.akka23Testkit,
  Library.akka23HttpTestkit,
  Library.junit,
  Library.scalaTest
)
