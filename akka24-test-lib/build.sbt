name := "akka24-test-lib"

libraryDependencies ++= List(
  Library.akka24Testkit,
  Library.akka24HttpTestkit,
  Library.junit,
  Library.scalaTest
)
