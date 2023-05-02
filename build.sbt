import sbt.Keys._

organization := "io.github.gaelrenoux"
name := "tranzactio"
licenses := Seq("APL2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))
description := "ZIO wrapper for Scala DB libraries (e.g. Doobie)"

val scala212Version = "2.12.15"
val scala213Version = "2.13.8"
val scala3Version = "3.2.2"
val supportedScalaVersions = List(scala212Version, scala213Version, scala3Version)

scalaVersion := scala3Version
crossScalaVersions := supportedScalaVersions



// Publishing information

import xerial.sbt.Sonatype._

sonatypeProjectHosting := Some(GitHubHosting("gaelrenoux", "tranzactio", "gael.renoux@gmail.com"))
publishTo := sonatypePublishTo.value

val allVersionsOption = Seq(
  "-encoding", "utf-8", // Specify character encoding used by source files.
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-explaintypes", // Explain type errors in more detail.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.

  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
  "-language:higherKinds", // Allow higher-kinded types
  "-language:implicitConversions" // Allow definition of implicit functions called views
)

val scala2AllVersionsOptions = Seq(
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

val scala213Options = scala2AllVersionsOptions ++ Seq(
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

val scala212Options = scala2AllVersionsOptions ++ Seq(
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

val scala3Options = Seq(
  "-Ysafe-init", // Check for uninitialized access.
)

scalacOptions ++= allVersionsOption ++ {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => scala213Options
    case Some((2, 12)) => scala212Options
    case Some((3, _)) => scala3Options
    case _ => Nil
  }
}


val ZioVersion = "2.0.8"
val ZioCatsVersion = "3.3.0"
val DoobieVersion = "1.0.0-RC2"
val AnormVersion = "2.7.0"
val H2Version = "2.1.214"

libraryDependencies ++= Seq(
  /* ZIO */
  "dev.zio" %% "zio" % ZioVersion,
  "dev.zio" %% "zio-streams" % ZioVersion,
  "dev.zio" %% "zio-interop-cats" % ZioCatsVersion,

  /* Doobie */
  "org.tpolecat" %% "doobie-core" % DoobieVersion % "optional",
  "org.playframework.anorm" %% "anorm" % AnormVersion % "optional",

  /* ZIO test */
  "dev.zio" %% "zio-test" % ZioVersion % "test",
  "dev.zio" %% "zio-test-sbt" % ZioVersion % "test",
  "dev.zio" %% "zio-test-magnolia" % ZioVersion % "test",

  /* H2 for tests */
  "com.h2database" % "h2" % H2Version % "test"
)


Test / fork := true
Test / testForkedParallel := true // run tests in parallel on the forked JVM
Test / testOptions += Tests.Argument("-oD") // show test duration

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

/* Adds samples as test sources */
Test / unmanagedSourceDirectories ++= Seq(
  new File("src/samples/scala")
)

/* Makes processes is SBT cancelable without closing SBT */
Global / cancelable := true
