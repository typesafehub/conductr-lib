name := "akka25-test-lib"

libraryDependencies ++= List(
  Library.akka25Testkit,
  Library.akka25StreamTestkit,
  Library.akka25HttpTestkit,
  Library.junit,
  Library.scalaTest
)
