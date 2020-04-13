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

  /** Generates the ConnectionSource layer.
   *
   * The H2 URL is based on an UUID, and the layer is executed for each test, so we have a different UUID on every test.
   *
   * Note that using H2, we need a delay to avoid dropping the DB when all connections are closed (between transactions
   * in a test). Another way to do this would be to handle a small connection pool (just one would be enough) but it
   * would make the test more complex. */
  private lazy val csLayer: ZLayer[ZEnv, Nothing, ConnectionSource] =
    ZLayer.fromFunctionManyManaged { env: ZEnv =>
      ConnectionSource.fromDriverManager(
        s"jdbc:h2:mem:${UUID.randomUUID().toString};DB_CLOSE_DELAY=10", "sa", "sa", errorStrategies = ErrorStrategies.Brutal
      ).build.provide(env)
    }

  val connectionCountSql = "select count(*) from information_schema.sessions"

  /** Generate the DB layer for that test, based on a connection source */
  val dbLayer: ZLayer[ConnectionSource with ZEnv, Nothing, Db]
}

object ITSpec {
  type ITEnv[Db <: Has[_]] = PersonQueries with Db with TestEnvironment
}
