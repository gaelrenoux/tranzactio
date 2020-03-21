package io.github.gaelrenoux.tranzactio.integration

import java.util.UUID

import io.github.gaelrenoux.tranzactio._
import io.github.gaelrenoux.tranzactio.integration.ITSpec.ITEnv
import samples.doobie.PersonQueries
import zio.duration._
import zio.test._
import zio.test.environment.{TestEnvironment, testEnvironment}
import zio.{Tagged, ZLayer, _}

abstract class ITSpec[Db <: Has[_] : Tagged] extends RunnableSpec[ITEnv[Db], Any] {
  type Spec = ZSpec[ITEnv[Db], Any]

  override def aspects: List[TestAspect[Nothing, ITEnv[Db], Nothing, Any]] = List(TestAspect.timeoutWarning(5.seconds))

  override def runner: TestRunner[ITEnv[Db], Any] = TestRunner(TestExecutor.default(itLayer))

  private lazy val itLayer: ULayer[ITEnv[Db]] =
    testEnvironment ++ PersonQueries.live ++ (testEnvironment >>> (ZEnv.any ++ csLayer) >>> dbLayer)

  /** Generates the ConnectionSource layer. Note that using H2, we need a delay to avoid dropping the DB when all
   * connections are closed. A better way would be to open a connection at the beginning of the test, and close it
   * afterwards. Also, the H2 URL is based on an UUID so that all tests have a different DB. To have a different UUID
   * for each test, we rebuild the layer every time. */
  private lazy val csLayer: ZLayer[ZEnv, Nothing, ConnectionSource] =
    ZLayer.fromFunctionManyManaged { env: ZEnv =>
      ConnectionSource.fromDriverManager(
        s"jdbc:h2:mem:${UUID.randomUUID().toString};DB_CLOSE_DELAY=10", "sa", "sa", errorStrategies = ErrorStrategies.Brutal
      ).build.provide(env)
    }

  /** Generate the DB layer for that test, based on a connection source */
  val dbLayer: ZLayer[ConnectionSource with ZEnv, Nothing, Db]
}

object ITSpec {
  type ITEnv[Db <: Has[_]] = PersonQueries with Db with TestEnvironment
}
