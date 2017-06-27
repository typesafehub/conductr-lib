import com.typesafe.sbt.SbtScalariform._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import com.typesafe.sbt.SbtPgp.autoImport._
import PgpKeys._
import xerial.sbt.Sonatype.autoImport._
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
    List(
      // Core settings
      organization := "com.typesafe.conductr",
      scalaVersion := Version.scala,
      crossScalaVersions := List(scalaVersion.value, "2.12.2"),
      scalacOptions ++= List(
        "-unchecked",
        "-deprecation",
        "-feature",
        "-language:_",
        "-target:jvm-1.8",
        "-encoding", "UTF-8"
      ),
      homepage := Some(url("http://conductr.lightbend.com/")),
      licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
      developers := List(
        Developer("conductr", "ConductR Library Contributors", "", url("https://github.com/typesafehub/conductr-lib/graphs/contributors"))
      ),
      scmInfo := Some(ScmInfo(url("https://github.com/typesafehub/conductr-lib"), "git@github.com:typesafehub/conductr-lib.git")),
      // Scalariform settings
      ScalariformKeys.preferences := ScalariformKeys.preferences.value
        .setPreference(AlignSingleLineCaseStatements, true)
        .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100),
      // Release settings
      releasePublishArtifactsAction := publishSigned.value,
      releaseCrossBuild := false,
      releaseIgnoreUntrackedFiles := true,
      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        inquireVersions,
        runClean,
        releaseStepCommandAndRemaining("+test"),
        setReleaseVersion,
        commitReleaseVersion,
        tagRelease,
        releaseStepCommandAndRemaining("+publishSigned"),
        releaseStepCommandAndRemaining("sonatypeReleaseAll"),
        setNextVersion,
        commitNextVersion,
        pushChanges
      )
    )
}
