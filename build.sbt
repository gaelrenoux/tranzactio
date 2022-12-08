import BuildHelper._

lazy val root = (project in file(".")).settings(
  name := "tranzactio-root",
  publish / skip := true,
).aggregate(core, anorm, doobie, examples)

lazy val core = project.settings(
  name := "tranzactio",
  description := "ZIO wrapper for Scala DB libraries (e.g. Doobie, Anorm)",
  stdSettings,
  publishSettings,
  libraryDependencies ++= coreDeps,
)

lazy val anorm = project.settings(
  name := "tranzactio-anorm",
  description := "ZIO wrapper for Anorm",
  stdSettings,
  publishSettings,
  libraryDependencies ++= anormDeps,
).dependsOn(core)

lazy val doobie = project.settings(
  name := "tranzactio-doobie",
  description := "ZIO wrapper for Doobie",
  stdSettings,
  publishSettings,
  libraryDependencies ++= doobieDeps,
).dependsOn(core)

lazy val examples = project.settings(
  stdSettings,
  publish / skip := true,
).dependsOn(
  // https://www.scala-sbt.org/1.x/docs/Multi-Project.html#Per-configuration+classpath+dependencies
  core % "test->compile;test->test",
  anorm % Test,
  doobie % Test,
)

/* Makes processes is SBT cancelable without closing SBT */
Global / cancelable := true
