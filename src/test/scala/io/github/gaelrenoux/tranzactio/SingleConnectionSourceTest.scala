package io.github.gaelrenoux.tranzactio

import zio.Fiber.Status
import zio.test._
import zio.{test => _, _}


object SingleConnectionSourceTest extends ZIOSpec[TestEnvironment with ConnectionSource] {
  type Env = TestEnvironment with ConnectionSource
  type MySpec = Spec[Env, Any]

  implicit private val errorStrategies: ErrorStrategies = ErrorStrategies.Nothing

  // TODO add aspect to timeout tests to 5 seconds

  override def bootstrap: ZLayer[Scope, Any, Env] = csLayer ++ testEnvironment

  lazy val csLayer: ZLayer[Scope, Nothing, ConnectionSource] = (JdbcLayers.connectionU ++ testEnvironment) >>> ConnectionSource.fromConnection

  val connectionCountSql = "select count(*) from information_schema.sessions"

  def spec: MySpec = suite("Single connection ConnectionSource Tests")(
    testDisallowConcurrentTasks
  )

  private val testDisallowConcurrentTasks: Spec[ConnectionSource, Nothing] = test("disallow concurrent tasks") {
    def query(trace: Ref[List[String]]) = {
      trace.update("start" :: _) *> ZIO.sleep(5.second) *> trace.update("end" :: _)
    }

    def runParallel(trace: Ref[List[String]]) =
      ZIO.service[ConnectionSource.Service].flatMap { cSource =>
        val op1 = cSource.runAutoCommit { _ => query(trace) }
        val op2 = cSource.runAutoCommit { _ => query(trace) }
        op1.zipPar(op2).mapError {
          case Left(e) => e
          case Right(_) => new RuntimeException("???")
        }.orDie
      }

    for {
      trace <- Ref.make[List[String]](Nil)
      forked <- runParallel(trace).fork
      _ <- TestClock.adjust(1.second).repeatWhileZIO(_ => forked.status.map(_ != Status.Done))
      _ <- forked.join
      result <- trace.get
    } yield assertTrue(result == "end" :: "start" :: "end" :: "start" :: Nil)
  }
}
