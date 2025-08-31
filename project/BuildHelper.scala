import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype._
import xerial.sbt.Sonatype.autoImport._

object BuildHelper {
  object V {
    val zio = "2.0.22"
    val zioCats = "23.1.0.5"
    val cats = "2.13.0"
    val doobie = "1.0.0-RC10"
    val anorm = "2.7.0"
    val h2 = "2.3.232"
    val scala212 = "2.12.20"
    val scala213 = "2.13.16"
    val scala3 = "3.7.2"
  }

  val coreDeps: Seq[ModuleID] = Seq(
    /* ZIO */
    "dev.zio" %% "zio" % V.zio,
    "dev.zio" %% "zio-streams" % V.zio,

    /* ZIO test */
    "dev.zio" %% "zio-test" % V.zio % Test,
    "dev.zio" %% "zio-test-sbt" % V.zio % Test,
    "dev.zio" %% "zio-test-magnolia" % V.zio % Test,

    /* H2 for tests */
    "com.h2database" % "h2" % V.h2 % Test
  )
  val anormDeps: Seq[ModuleID] = Seq(
    "org.playframework.anorm" %% "anorm" % V.anorm,
  )
  val doobieDeps: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio-interop-cats" % V.zioCats,
    "org.typelevel" %% "cats-core" % V.cats,
    "org.tpolecat" %% "doobie-core" % V.doobie,
  )

  val stdSettings: Seq[Setting[_]] = Seq(
    scalaVersion := V.scala3,
    crossScalaVersions := Seq(V.scala212, V.scala213, V.scala3),
    scalacOptions ++= scalaVersion(ScalacOptions(_)).value,
    Test / fork := true,
    Test / testForkedParallel := true, // run tests in parallel on the forked JVM
    Test / testOptions += Tests.Argument("-oD"), // show test duration
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )

  val publishSettings: Seq[Setting[_]] = Seq(
    organization := "io.github.gaelrenoux",
    licenses := Seq ("APL2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    sonatypeProjectHosting := Some(GitHubHosting("gaelrenoux", "tranzactio", "gael.renoux@gmail.com")),
    publishTo := sonatypePublishTo.value,
  )

}

object ScalacOptions {
  private val allVersionsOption = Seq(
    "-encoding", "utf-8", // Specify character encoding used by source files.
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-explaintypes", // Explain type errors in more detail.
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.

    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
    "-language:higherKinds", // Allow higher-kinded types
    "-language:implicitConversions" // Allow definition of implicit functions called views
  )

  private val scala2AllVersionsOptions = Seq(
    "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
    "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
    "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
    "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
    "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
    "-Xlint:option-implicit", // Option.apply used implicit view.
    "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow", //  A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow" // A local type parameter shadows a type already in scope.
  )

  private val scala213Options = scala2AllVersionsOptions ++ Seq(
    "-Ymacro-annotations",

    "-Wdead-code", // Warn when dead code is identified.
    "-Wextra-implicit", // Warn when more than one implicit parameter section is defined.
    "-Wnumeric-widen", // Warn when numerics are widened.
    // "-Woctal-literal", // Warn on obsolete octal syntax. // false positive on 0 since Scala 2.13.2
    "-Wunused:explicits", // Warn if an explicit parameter is unused.
    "-Wunused:implicits", // Warn if an implicit parameter is unused.
    "-Wunused:imports", // Warn when imports are unused.
    "-Wunused:locals", // Warn if a local definition is unused.
    "-Wunused:patvars", // Warn if a variable bound in a pattern is unused.
    "-Wunused:privates", // Warn if a private member is unused.
    "-Wvalue-discard", // Warn when non-Unit expression results are unused.

    "-Xlint:deprecation", // Enable linted deprecations.
    "-Xlint:eta-sam", // Warn on eta-expansion to meet a Java-defined functional interface that is not explicitly annotated with @FunctionalInterface.
    "-Xlint:eta-zero", // Warn on eta-expansion (rather than auto-application) of zero-ary method.
    "-Xlint:implicit-not-found", // Check @implicitNotFound and @implicitAmbiguous messages.
    // "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`. // Happens too much. See https://github.com/zio/zio/pull/6455.
    "-Xlint:nonlocal-return", // A return statement used an exception for flow control.
    "-Xlint:serial", // @SerialVersionUID on traits and non-serializable classes.
    "-Xlint:unused", // Enable -Ywarn-unused:imports,privates,locals,implicits.
    "-Xlint:valpattern" // Enable pattern checks in val definitions.
  )

  private val scala212Options = scala2AllVersionsOptions ++ Seq(
    "-language:experimental.macros", // Allow macro definition (besides implementation and application)
    "-Ypartial-unification", // Enable partial unification in type constructor inference

    "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ywarn-dead-code", // Warn when dead code is identified.
    "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
    "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit", // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen", // Warn when numerics are widened.
    "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals", // Warn if a local definition is unused.
    "-Ywarn-unused:params", // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates", // Warn if a private member is unused.
    "-Ywarn-value-discard", // Warn when non-Unit expression results are unused.

    // "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`. // false positives in 2.12
    "-Xlint:unsound-match" // Pattern match may not be typesafe.
  )

  private val scala3Options = Seq(
    "-Ysafe-init", // Check for uninitialized access.
  )

  def apply(scalaVersion: String): Seq[String] = allVersionsOption ++ {
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 13)) => scala213Options
      case Some((2, 12)) => scala212Options
      case Some((3, _)) => scala3Options
      case _ => Nil
    }
  }
}
