package io.github.gaelrenoux.tranzactio

import zio._
import zio.duration._
import zio.test.Assertion._
import zio.test._
import zio.test.environment._

object SingleConnectionSourceTest extends RunnableSpec[TestEnvironment with ConnectionSource, Any] {
  type Env = TestEnvironment with ConnectionSource
  type Spec = ZSpec[Env, Any]

  implicit private val errorStrategies: ErrorStrategies = ErrorStrategies.Brutal

  override def aspects: List[TestAspect[Nothing, Env, Nothing, Any]] = List(TestAspect.timeout(5.seconds))

  override def runner: TestRunner[Env, Any] = TestRunner(TestExecutor.default(testEnvironment ++ csLayer))

  lazy val csLayer: ULayer[ConnectionSource] = (JdbcLayers.connectionU ++ testEnvironment) >>> ConnectionSource.fromConnection

  val connectionCountSql = "select count(*) from information_schema.sessions"

  def spec: Spec = suite("Single connection ConnectionSource Tests")(
    testDisallowConcurrentTasks
  )

  private val testDisallowConcurrentTasks = testM("disallow concurrent tasks") {
    def run(ref: Ref[Int]) =
      ZIO.service[ConnectionSource.Service].flatMap { con =>
        val op1 = con.runAutoCommit(_ => ref.update(_ + 1).delay(2.seconds))
        val op2 = con.runAutoCommit(_ => ref.update(_ + 1).delay(2.seconds))
        op1.zipPar(op2).mapError {
          case Left(e) => e
          case Right(_) => new RuntimeException("???")
        }.orDie
      }

    for {
      check <- Ref.make(0)
      _ <- run(check).fork
      _ <- TestClock.adjust(3.seconds)
      firstValue <- check.get
      _ <- TestClock.adjust(2.seconds)
      secondValue <- check.get
    } yield
      assert(firstValue)(equalTo(1)) &&
        assert(secondValue)(equalTo(2))
  }

}
