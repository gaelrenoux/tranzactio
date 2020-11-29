package samples

import zio.test.Assertion._
import zio.test.environment._
import zio.test.{DefaultRunnableSpec, ZSpec, _}

/** Run all samples as ZIO tests */
object SamplesSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("SamplesSpec")(
    testApp("Doobie", doobie.LayeredApp),
    testApp("Doobie-Streaming", doobie.LayeredAppStreaming),
    testApp("Anorm", anorm.LayeredApp)
  )

  private def testApp(name: String, app: zio.App) =
    testM(s"$name LayeredApp prints its progress then the trio") {
      for {
        _ <- app.run(Nil)
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
