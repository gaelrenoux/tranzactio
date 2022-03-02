package samples

import zio.{Chunk, ZEnv, ZIO, ZIOAppArgs, ZLayer}
import zio.test.Assertion._
import zio.test.{ZSpec, _}
import zio.test.ZIOSpecDefault

/** Run all samples as ZIO tests */
object SamplesSpec extends ZIOSpecDefault {
  private val ignoredAppArgs = ZLayer.succeed(ZIOAppArgs.apply(Chunk.empty))

  override def spec =
     suite("SamplesSpec")(
       testApp("Doobie", doobie.LayeredApp),
       testApp("Doobie-Streaming", doobie.LayeredAppStreaming),
       testApp("Anorm", anorm.LayeredApp)
     )

  private def testApp(name: String, app: zio.ZIOAppDefault): ZSpec[TestConsole with ZEnv, Any] =



     test(s"$name LayeredApp prints its progress then the trio") {
       for {
         // Now that ZIO specs extend ZIOApp, we have to provide the ZIOAppArgs, even though most specs won't need them.
         _ <- app.run.provideCustom(ignoredAppArgs)
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
