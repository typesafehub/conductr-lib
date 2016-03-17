name := "conductr-bundle-lib"

javacOptions in compile ++= List("-encoding", "UTF-8", "-source", "1.6", "-target", "1.6", "-Xlint:unchecked", "-Xlint:deprecation")
javacOptions in doc ++= List("-encoding", "UTF-8", "-source", "1.6")

unmanagedSourceDirectories in Compile := List((javaSource in Compile).value)

autoScalaLibrary := false
crossPaths := false

parallelExecution in Test := true
