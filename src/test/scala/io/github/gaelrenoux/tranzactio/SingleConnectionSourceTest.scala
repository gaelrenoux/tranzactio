package io.github.gaelrenoux.tranzactio

import zio._
import zio.duration._
import zio.test.Assertion._
import zio.test._
import zio.test.environment._

object SingleConnectionSourceTest extends RunnableSpec[TestEnvironment with ConnectionSource, Any] {
  type Env = TestEnvironment with ConnectionSource
  type Spec = ZSpec[Env, Any]

  implicit private val errorStrategies: ErrorStrategies = ErrorStrategies.Nothing

  override def aspects: List[TestAspect[Nothing, Env, Nothing, Any]] = List(TestAspect.timeout(5.seconds))

  override def runner: TestRunner[Env, Any] = TestRunner(TestExecutor.default(testEnvironment ++ csLayer))

  lazy val csLayer: ULayer[ConnectionSource] = (JdbcLayers.connectionU ++ testEnvironment) >>> ConnectionSource.fromConnection

  val connectionCountSql = "select count(*) from information_schema.sessions"

  def spec: Spec = suite("Single connection ConnectionSource Tests")(
    testDisallowConcurrentTasks
  )

  private val testDisallowConcurrentTasks = testM("disallow concurrent tasks") {
    def query(trace: Ref[List[String]]) = {
      trace.update(s"start" :: _) *> ZIO.sleep(5.second) *> trace.update(s"end" :: _)
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
      _ <- TestClock.adjust(1.second).repeatWhileM(_ => forked.status.map(!_.isDone))
      _ <- forked.join
      result <- trace.get
    } yield assert(result)(equalTo("end" :: "start" :: "end" :: "start" :: Nil))
  }

}
