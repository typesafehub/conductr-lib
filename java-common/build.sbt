name := "java-conductr-lib-common"

javacOptions in compile ++= List("-encoding", "UTF-8", "-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-Xlint:deprecation")
javacOptions in doc ++= List("-encoding", "UTF-8", "-source", "1.8")

unmanagedSourceDirectories in Compile := List((javaSource in Compile).value)

autoScalaLibrary := false
crossPaths := false
crossScalaVersions := List(crossScalaVersions.value.head) // Requires building just the once
