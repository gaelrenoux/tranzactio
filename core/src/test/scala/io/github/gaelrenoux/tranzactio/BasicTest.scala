package io.github.gaelrenoux.tranzactio

import zio.stream.ZStream
import zio.test._
import zio.{test => _, _}

import java.sql.Connection


object BasicTest extends ZIOSpec[TestEnvironment] {
  type Env = TestEnvironment
  type MySpec = Spec[Env, Any]

  implicit private val errorStrategies: ErrorStrategies = ErrorStrategies.Nothing

  override def bootstrap: ZLayer[Any, Any, Env] = testEnvironment

  val connectionCountSql = "select count(*) from information_schema.sessions"

  def connectionCountQuery(c: Connection): ZIO[Any, Throwable, Int] = ZIO.attemptBlocking {
    val stmt = c.prepareStatement(connectionCountSql)
    try {
      val rs = stmt.executeQuery()
      rs.next()
      val count = rs.getInt(1)
      rs.close()
      count
    } finally {
      stmt.close()
    }
  }

  def connectionCountQueryStream(c: Connection): ZStream[Any, Throwable, Int] =
    ZStream.fromZIO(connectionCountQuery(c))

  def spec: MySpec = suite("Single connection ConnectionSource Tests")(
    testRunTransactionStreamFailureOnOpen,
    testRunTransactionStreamFailureOnCommit,
    testRunTransactionStreamFailureOnClose,
    testDieInOpen,
    testDieInProcess,
    testDieInClose,
    testDieInCloseAfterFailure
  )

  private val testRunTransactionStreamFailureOnOpen = test("runTransactionStream failure > on open") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnOpen = true)
    val zio: ZStream[Any, Either[DbException, Throwable], Int] = cs.runTransactionStream(connectionCountQueryStream)
    zio.runCollect.flip.map { e =>
      assertTrue(e == Left(DbException.Wrapped(FailingConnectionSource.OpenException)))
    }
  }

  private val testRunTransactionStreamFailureOnCommit = test("runTransactionStream failure > on commit (after success)") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnCommit = true)
    val zio: ZStream[Any, Either[DbException, Throwable], Int] = cs.runTransactionStream(connectionCountQueryStream)
    zio.runCollect.flip.map { e =>
      assertTrue(e == Left(DbException.Wrapped(FailingConnectionSource.CommitException)))
    }
  }

  private val testRunTransactionStreamFailureOnClose = test("runTransactionStream failure > on close") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnClose = true)
    val zio: ZStream[Any, Either[DbException, Throwable], Int] = cs.runTransactionStream(connectionCountQueryStream)
    zio.runCollect.cause.map {
      case Cause.Die(ex, _) => assertTrue(ex == DbException.Wrapped(FailingConnectionSource.CloseException))
    }
  }

  private val testDieInOpen = test("#1 Die in the open part") {
    val stream = ZStream
      .acquireReleaseWith(ZIO.debug("#1 Open") *> ZIO.die(new Exception("Some error")).unit)(_ => ZIO.debug("#1 Close"))
      .crossRight(ZStream.fromZIO(ZIO.debug("#1 Process")))
      .catchSomeCause {
        case Cause.Die(ex, stack) => ZStream.failCause(Cause.Fail(ex, stack))
      }
    stream.runCollect.flip.map { e =>
      assertTrue(e.getMessage == "Some error")
    }
  }

  private val testDieInProcess = test("#2 Die in the process part") {
    val stream = ZStream
      .acquireReleaseWith(ZIO.debug("#2 Open"))(_ => ZIO.debug("#2 Close"))
      .crossRight(ZStream.fromZIO(ZIO.debug("#2 Process")) ++ ZStream.die(new Exception("Some error")))
      .catchSomeCause {
        case Cause.Die(ex, stack) => ZStream.failCause(Cause.Fail(ex, stack))
      }
    stream.runCollect.flip.map { e =>
      assertTrue(e.getMessage == "Some error")
    }
  }

  private val testDieInClose = test("#3 Die in the close part") {
    val stream = ZStream
      .acquireReleaseWith(ZIO.debug("#3 Open"))(_ => ZIO.debug("#3 Close") *> ZIO.die(new Exception("Some error")).unit)
      .crossRight(ZStream.fromZIO(ZIO.debug("#3 Process")))
      .catchSomeCause {
        case Cause.Die(ex, stack) => ZStream.failCause(Cause.Fail(ex, stack))
      }
    stream.runCollect.flip.map { e =>
      assertTrue(e.getMessage == "Some error")
    }
  }

  private val testDieInCloseAfterFailure = test("#4 Die in the close part (after a failure)") {
    val stream = ZStream
      .acquireReleaseWith(ZIO.debug("#4 Open"))(_ => ZIO.debug("#4 Close") *> ZIO.die(new Exception("Some error")).unit)
      .crossRight(ZStream.fromZIO(ZIO.debug("#4 Process") *> ZIO.fail(new Exception("Some other error"))))
      .catchSomeCause {
        case Cause.Die(ex, stack) => ZStream.failCause(Cause.Fail(ex, stack))
      }
    stream.runCollect.flip.map { e =>
      assertTrue(e.getMessage == "Some error")
    }
  }

}
