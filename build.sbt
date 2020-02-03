import sbt.Keys._

organization := "gaelrenoux"
name := "tranzactio"
version := "0.1-SNAPSHOT"

// update artima supersafe when a version is released (and check xdotai/diff once in a while)
scalaVersion := "2.13.1"

scalacOptions ++= Seq(
  "-Ymacro-annotations",

  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",

  // "-XX:MaxInlineLevel=18", // see https://github.com/scala/bug/issues/11627#issuecomment-514619316

  "-explaintypes", // Explain type errors in more detail.
  "-Werror", // Fail the compilation if there are any warnings.

  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
  //  "-Xdev", // Indicates user is a developer - issue warnings about anything which seems amiss

  "-Wdead-code", // Warn when dead code is identified.
  "-Wextra-implicit", // Warn when more than one implicit parameter section is defined.
  // "-Wmacros:before", // Enable lint warnings on macro expansions. Default: before, help to list choices.
  "-Wnumeric-widen", // Warn when numerics are widened.
  "-Woctal-literal", // Warn on obsolete octal syntax.
  // "-Wself-implicit", // this detects to much (see https://github.com/scala/bug/issues/10760 for original justification)
  "-Wunused:explicits", // Warn if an explicit parameter is unused.
  "-Wunused:implicits", // Warn if an implicit parameter is unused.
  "-Wunused:imports", // Warn when imports are unused.
  "-Wunused:locals", // Warn if a local definition is unused.
  "-Wunused:patvars", // Warn if a variable bound in a pattern is unused.
  "-Wunused:privates", // Warn if a private member is unused.
  "-Wvalue-discard", // Warn when non-Unit expression results are unused.

  // "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver. // this is fine
  "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
  "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
  "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
  "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
  "-Xlint:private-shadow", //  A private field (or class parameter) shadows a superclass field.
  "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
  "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:option-implicit", // Option.apply used implicit view.
  "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
  // "-Xlint:package-object-classes", // Class or object defined in package object. // this is fine
  "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
  "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:unused", // Enable -Ywarn-unused:imports,privates,locals,implicits.
  "-Xlint:nonlocal-return", // A return statement used an exception for flow control.
  "-Xlint:implicit-not-found", // Check @implicitNotFound and @implicitAmbiguous messages.
  "-Xlint:serial", // @SerialVersionUID on traits and non-serializable classes.
  "-Xlint:valpattern", // Enable pattern checks in val definitions.
  "-Xlint:eta-zero", // Warn on eta-expansion (rather than auto-application) of zero-ary method.
  "-Xlint:eta-sam", // Warn on eta-expansion to meet a Java-defined functional interface that is not explicitly annotated with @FunctionalInterface.
  "-Xlint:deprecation" // Enable linted deprecations.
)


resolvers ++= Seq(
  "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Secured Central Repository" at "https://repo1.maven.org/maven2",
  Resolver.sonatypeRepo("snapshots")
)

val ZioVersion = "1.0.0-RC17"
val ZioCatsVersion = "2.0.0.0-RC10"
val ZioMacroVersion = "0.6.0"
val DoobieVersion = "0.8.6"

libraryDependencies ++= Seq(
  /* ZIO */
  "dev.zio" %% "zio" % ZioVersion,
  "dev.zio" %% "zio-interop-cats" % ZioCatsVersion,
  "dev.zio" %% "zio-macros-core" % ZioMacroVersion,
  "dev.zio" %% "zio-macros-test" % ZioMacroVersion,

  /* Doobie */
  "org.tpolecat" %% "doobie-core" % DoobieVersion % "optional",

  /* ZIO test */
  "dev.zio" %% "zio-test" % ZioVersion % "test",
  "dev.zio" %% "zio-test-sbt" % ZioVersion % "test"
)

Test / fork := true
Test / testForkedParallel := true // run tests in parallel on the forked JVM
Test / testOptions += Tests.Argument("-oD") // show test duration

//testFrameworks += Seq(new TestFramework("zio.test.sbt.ZTestFramework"))

/* Adds samples as test sources */
Test / unmanagedSourceDirectories ++= Seq(
  new File("src/samples/scala")
)

/* Makes processes is SBT cancelable without closing SBT */
Global / cancelable := true
