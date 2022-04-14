package samples

import zio.test.Assertion._
import zio.test.{ZSpec, _}
import zio.{Chunk, Scope, ULayer, ZIOAppArgs, ZIOAppDefault, ZLayer}

/** Run all samples as ZIO tests */
object SamplesSpec extends DefaultRunnableSpec {
  type Spec = ZSpec[TestEnvironment, Any]

  private val ignoredAppArgs: ULayer[ZIOAppArgs] = ZLayer.succeed(ZIOAppArgs.apply(Chunk.empty))

  override def spec: Spec =
    suite("SamplesSpec")(
      testApp("Doobie", doobie.LayeredApp),
      testApp("Doobie-Streaming", doobie.LayeredAppStreaming),
      testApp("Anorm", anorm.LayeredApp)
    )

  private def testApp(name: String, app: ZIOAppDefault): Spec =

    test(s"$name LayeredApp prints its progress then the trio") {
      for {
        _ <- app.run.provideCustom(ignoredAppArgs ++ Scope.default)
        output <- TestConsole.output
      } yield assert(output)(equalTo(Vector(
        "Starting the app\n",
        "Creating the table\n",
        "Inserting the trio\n",
        "Reading the trio\n",
        "Buffy Summers, Willow Rosenberg, Alexander Harris\n"
      )))
    }

}
