/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

name := "conductr-bundle-lib"

javacOptions in compile ++= List("-encoding", "UTF-8", "-source", "1.6", "-target", "1.6", "-Xlint:unchecked", "-Xlint:deprecation")
javacOptions in doc ++= List("-encoding", "UTF-8", "-source", "1.6")

unmanagedSourceDirectories in Compile := List((javaSource in Compile).value)

autoScalaLibrary := false
crossPaths := false
