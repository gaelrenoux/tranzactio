package samples

import zio.test.Assertion._

import zio.test.{ZSpec, _}
import zio.test.ZIOSpecDefault

/** Run all samples as ZIO tests */
object SamplesSpec extends ZIOSpecDefault {


  // TODO: solve
//   type mismatch;
//  found   : zio.test.Spec[zio.Clock with zio.Console with zio.System with zio.Random with zio.ZIOAppArgs with zio.test.TestConsole,zio.test.TestFailure[Any],zio.test.TestSuccess]
//  required: zio.test.ZSpec[zio.test.TestEnvironment,Any]
//     (which expands to)  zio.test.Spec[zio.test.Annotations with zio.test.Live with zio.test.Sized with zio.test.TestClock with zio.test.TestConfig with zio.test.TestConsole with zio.test.TestRandom with zio.test.TestSystem,zio.test.TestFailure[Any],zio.test.TestSuccess]

  override def spec: ZSpec[TestEnvironment, Any] = ???
  // suite("SamplesSpec")(
  //   testApp("Doobie", doobie.LayeredApp),
  //   testApp("Doobie-Streaming", doobie.LayeredAppStreaming),
  //   testApp("Anorm", anorm.LayeredApp)
  // )

  private def testApp(name: String, app: zio.ZIOAppDefault) = ???
    // test(s"$name LayeredApp prints its progress then the trio") {
    //   for {
    //     _ <- app.run
    //     output <- TestConsole.output
    //   } yield assert(output)(equalTo(Vector(
    //     "Starting the app\n",
    //     "Creating the table\n",
    //     "Inserting the trio\n",
    //     "Reading the trio\n",
    //     "Buffy Summers, Willow Rosenberg, Alexander Harris\n"
    //   )))
    // }

}
