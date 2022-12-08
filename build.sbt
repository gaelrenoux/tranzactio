import BuildHelper._

lazy val tranzactio = (project in file(".")).settings(
  name := "tranzactio",
  description := "ZIO wrapper for Scala DB libraries (e.g. Doobie, Anorm)",
  stdSettings,
  publishSettings,
  /* Adds samples as test sources */
  Test / unmanagedSourceDirectories ++= Seq(
    baseDirectory.value /"src/samples/scala"
  ),
)

/* Makes processes is SBT cancelable without closing SBT */
Global / cancelable := true
