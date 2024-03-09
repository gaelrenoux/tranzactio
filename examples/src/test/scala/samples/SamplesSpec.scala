package samples

import zio.test.{Spec, _}
import zio.{Chunk, Scope, ULayer, ZIOAppArgs, ZIOAppDefault, ZLayer}

/** Run all samples as ZIO tests */
object SamplesSpec extends ZIOSpecDefault {
  type MySpec = Spec[TestEnvironment, Any]

  private val ignoredAppArgs: ULayer[ZIOAppArgs] = ZLayer.succeed(ZIOAppArgs.apply(Chunk.empty))

  override def spec: MySpec =
    suite("SamplesSpec")(
      testApp("Doobie", doobie.LayeredApp),
      testApp("Doobie-Streaming", doobie.LayeredAppStreaming),
      testAppMultipleDatabases("Doobie", doobie.LayeredAppMultipleDatabases),
      testApp("Anorm", anorm.LayeredApp),
      testAppMultipleDatabases("Anorm", anorm.LayeredAppMultipleDatabases)
    )

  private def testApp(name: String, app: ZIOAppDefault): MySpec =

    test(s"$name LayeredApp prints its progress then the trio") {
      for {
        _ <- app.run.provide(ignoredAppArgs ++ Scope.default)
        output <- TestConsole.output
      } yield assertTrue(output == Vector(
        "Starting the app\n",
        "Creating the table\n",
        "Inserting the trio\n",
        "Reading the trio\n",
        "Buffy Summers, Willow Rosenberg, Alexander Harris\n"
      ))
    }

  private def testAppMultipleDatabases(name: String, app: ZIOAppDefault): MySpec =

    test(s"$name LayeredAppMultipleDatabases prints its progress for the trio, then its progress for the mentor, then the team") {
      for {
        _ <- app.run.provide(ignoredAppArgs ++ Scope.default)
        output <- TestConsole.output
      } yield assertTrue(output == Vector(
        "Starting the app\n",
        "Creating the table\n",
        "Inserting the trio\n",
        "Reading the trio\n",
        "Creating the table\n",
        "Inserting the mentor\n",
        "Reading the mentor\n",
        "Buffy Summers, Willow Rosenberg, Alexander Harris, Rupert Giles\n"
      ))
    }

}
