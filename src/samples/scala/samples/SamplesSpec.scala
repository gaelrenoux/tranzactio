package samples

import zio.test.Assertion._
import zio.test.environment._
import zio.test.{DefaultRunnableSpec, ZSpec, _}

/** Run all samples as ZIO tests */
object SamplesSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("SamplesSpec")(

    testM("Doobie LayeredApp prints its progress then the trio") {
      for {
        _ <- doobie.LayeredApp.run(Nil)
        output <- TestConsole.output
      } yield assert(output)(equalTo(Vector(
        "Loading configuration\n",
        "Setting up the env\n",
        "Calling the app\n",
        "Creating the table\n",
        "Inserting the trio\n",
        "Reading the trio\n",
        "Buffy Summers, Willow Rosenberg, Alexander Harris\n"
      )))
    },

    testM("Anorm LayeredApp prints its progress then the trio") {
      for {
        _ <- anorm.LayeredApp.run(Nil)
        output <- TestConsole.output
      } yield assert(output)(equalTo(Vector(
        "Loading configuration\n",
        "Setting up the env\n",
        "Calling the app\n",
        "Creating the table\n",
        "Inserting the trio\n",
        "Reading the trio\n",
        "Buffy Summers, Willow Rosenberg, Alexander Harris\n"
      )))
    }

  )

}
