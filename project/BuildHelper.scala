import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype._
import xerial.sbt.Sonatype.autoImport._

object BuildHelper {
  object V {
    val zio = "2.1.26"
    val zioCats = "23.1.0.13"
    val cats = "2.13.0"
    val doobie = "1.0.0-RC13"
    val anorm = "3.1.0"
    val h2 = "2.4.240"
    val scala213 = "2.13.18"
    val scala3 = "3.3.8" // latest LTS version
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
    "org.typelevel" %% "doobie-core" % V.doobie,
  )

  val stdSettings: Seq[Setting[_]] = Seq(
    scalaVersion := V.scala3,
    crossScalaVersions := Seq(V.scala213, V.scala3),
    scalacOptions ++= scalaVersion(ScalacOptions(_)).value,
    Test / fork := true,
    Test / testForkedParallel := true, // run tests in parallel on the forked JVM
    Test / testOptions += Tests.Argument("-oD"), // show test duration
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )

  val publishSettings: Seq[Setting[_]] = Seq(
    organization := "io.github.gaelrenoux",
    licenses := Seq(License.Apache2),
    scmInfo := Some(ScmInfo(
      url("https://github.com/gaelrenoux/tranzactio"),
      "scm:git@github.com:gaelrenoux/tranzactio.git"
    )),
    homepage := Some(url("https://github.com/gaelrenoux/tranzactio")),
    developers := List(
      Developer(
        id = "gaelrenoux",
        name = "Gaël Renoux",
        email = "gael.renoux@gmail.com",
        url = url("https://github.com/gaelrenoux")
      )
    ),
    publishTo := sonatypePublishToBundle.value,
    sonatypeCredentialHost := sonatypeCentralHost
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

  private val scala3Options = Seq(
    "-Ysafe-init", // Check for uninitialized access.
  )

  def apply(scalaVersion: String): Seq[String] = allVersionsOption ++ {
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 13)) => scala213Options
      case Some((3, _)) => scala3Options
      case _ => Nil
    }
  }
}
