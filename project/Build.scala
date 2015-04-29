/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

import bintray.Plugin.bintrayPublishSettings
import bintray.Keys._
import com.typesafe.sbt.SbtScalariform._
import de.heikoseeberger.sbtheader.SbtHeader.autoImport._
import sbtrelease.ReleasePlugin._
import sbt._
import sbt.Keys._
import scalariform.formatter.preferences._

object Build extends AutoPlugin {

  override def requires =
    plugins.JvmPlugin

  override def trigger =
    allRequirements

  override def projectSettings =
    scalariformSettings ++
    inConfig(Compile)(compileInputs.in(compile) <<= compileInputs.in(compile).dependsOn(createHeaders.in(compile))) ++
    inConfig(Test)(compileInputs.in(compile) <<= compileInputs.in(compile).dependsOn(createHeaders.in(compile))) ++
    releaseSettings ++
    bintrayPublishSettings ++
    List(
      // Core settings
      organization := "com.typesafe.conductr",
      scalaVersion := Version.scala,
      crossScalaVersions := List(scalaVersion.value),
      scalacOptions ++= List(
        "-unchecked",
        "-deprecation",
        "-feature",
        "-language:_",
        "-target:jvm-1.6",
        "-encoding", "UTF-8"
      ),
      // Scalariform settings
      ScalariformKeys.preferences := ScalariformKeys.preferences.value
        .setPreference(AlignSingleLineCaseStatements, true)
        .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
        .setPreference(DoubleIndentClassDeclaration, true)
        .setPreference(PreserveDanglingCloseParenthesis, true),
      // Header settings
      headers := Map(
        "scala" -> (
          HeaderPattern.cStyleBlockComment,
          """|/*
            | * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
            | * No information contained herein may be reproduced or transmitted in any form
            | * or by any means without the express written permission of Typesafe, Inc.
            | */
            |
            |""".stripMargin
          ),
        "py" -> (
          HeaderPattern.hashLineComment,
          """|# Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
            |# No information contained herein may be reproduced or transmitted in any form
            |# or by any means without the express written permission of Typesafe, Inc.
            |
            |""".stripMargin
          ),
        "conf" -> (
          HeaderPattern.hashLineComment,
          """|# Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
            |# No information contained herein may be reproduced or transmitted in any form
            |# or by any means without the express written permission of Typesafe, Inc.
            |
            |""".stripMargin
          )
      ),
      // Bintray settings
      bintrayOrganization in bintray := Some("typesafe"),
      repository in bintray := "maven-releases",
      // Release settings
      ReleaseKeys.versionBump := sbtrelease.Version.Bump.Minor
    )
}
