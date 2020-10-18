package io.github.gaelrenoux.tranzactio.integration

import io.github.gaelrenoux.tranzactio._
import io.github.gaelrenoux.tranzactio.integration.ITSpec.ITEnv
import zio.duration._
import zio.test._
import zio.test.environment.{TestEnvironment, testEnvironment}
import zio.{Tag, ZLayer, _}

abstract class ITSpec[Db <: Has[_] : Tag, PersonQueries <: Has[_] : Tag] extends RunnableSpec[ITEnv[Db, PersonQueries], Any] {
  type Spec = ZSpec[ITEnv[Db, PersonQueries], Any]

  implicit val errorStrategies: ErrorStrategies = ErrorStrategies.Nothing

  override def aspects: List[TestAspect[Nothing, ITEnv[Db, PersonQueries], Nothing, Any]] = List(TestAspect.timeout(5.seconds))

  override def runner: TestRunner[ITEnv[Db, PersonQueries], Any] = TestRunner(TestExecutor.default(itLayer))

  private lazy val itLayer: ULayer[ITEnv[Db, PersonQueries]] = {
    val uCsLayer = (testEnvironment ++ JdbcLayers.datasourceU) >>> ConnectionSource.fromDatasource
    val uDbLayer = (uCsLayer ++ testEnvironment) >>> dbLayer
    testEnvironment ++ personQueriesLive ++ uDbLayer
  }

  val connectionCountSql = "select count(*) from information_schema.sessions"

  /** Generate the DB layer for that test, based on a connection source */
  val dbLayer: ZLayer[ConnectionSource with ZEnv, Nothing, Db]

  /** Provides the PersonQueries */
  val personQueriesLive: ULayer[PersonQueries]
}

object ITSpec {
  type ITEnv[Db <: Has[_], PersonQueries <: Has[_]] = PersonQueries with Db with TestEnvironment
}
