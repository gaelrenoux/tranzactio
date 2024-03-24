package io.github.gaelrenoux.tranzactio

import zio.stream.ZStream
import zio.test._
import zio.{test => _, _}

import java.sql.Connection


object ConnectionSourceTest extends ZIOSpec[TestEnvironment] {
  type Env = TestEnvironment
  type MySpec = Spec[Env, Any]

  implicit private val errorStrategies: ErrorStrategies = ErrorStrategies.Nothing

  // TODO add aspect to timeout tests to 5 seconds

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
    testRunTransactionFailureOnOpen,
    testRunTransactionFailureOnAutoCommit,
    testRunTransactionFailureOnCommit,
    testRunTransactionFailureOnCommitAfterFailure,
    testRunTransactionFailureOnRollback,
    testRunTransactionFailureOnClose,
    testRunAutoCommitFailureOnOpen,
    testRunAutoCommitFailureOnAutoCommit,
    testRunAutoCommitFailureOnClose,
    testRunTransactionStreamFailureOnOpen,
    testRunTransactionStreamFailureOnAutoCommit,
    testRunTransactionStreamFailureOnCommit,
    testRunTransactionStreamFailureOnCommitAfterFailure,
    testRunTransactionStreamFailureOnRollback,
    testRunTransactionStreamFailureOnClose,
    testRunAutoCommitStreamFailureOnOpen,
    testRunAutoCommitStreamFailureOnAutoCommit,
    testRunAutoCommitStreamFailureOnClose
  )

  private val testRunTransactionFailureOnOpen = test("runTransaction failure > on open") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnOpen = true)
    val zio: ZIO[Any, Either[DbException, Throwable], Int] = cs.runTransaction(connectionCountQuery)
    zio.flip.map { e =>
      assertTrue(e == Left(DbException.Wrapped(FailingConnectionSource.OpenException)))
    }
  }

  private val testRunTransactionFailureOnAutoCommit = test("runTransaction failure > on auto-commit") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnAutoCommit = true)
    val zio: ZIO[Any, Either[DbException, Throwable], Int] = cs.runTransaction(connectionCountQuery)
    zio.flip.map { e =>
      assertTrue(e == Left(DbException.Wrapped(FailingConnectionSource.AutoCommitException)))
    }
  }

  private val testRunTransactionFailureOnCommit = test("runTransaction failure > on commit") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnCommit = true)
    val zio: ZIO[Any, Either[DbException, Throwable], Int] = cs.runTransaction(connectionCountQuery)
    zio.flip.map { e =>
      assertTrue(e == Left(DbException.Wrapped(FailingConnectionSource.CommitException)))
    }
  }

  private val testRunTransactionFailureOnCommitAfterFailure = test("runTransaction failure > on commit (after failure)") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnCommit = true)
    val zio: ZIO[Any, Either[DbException, String], Int] = cs.runTransaction(_ => ZIO.fail("Not a good query"), commitOnFailure = true)
    zio.cause.map {
      case Cause.Then(Cause.Fail(firstError, _), Cause.Fail(secondError, _)) =>
        assertTrue(
          firstError == Right("Not a good query"),
          secondError == Left(DbException.Wrapped(FailingConnectionSource.CommitException))
        )
    }
  }

  private val testRunTransactionFailureOnRollback = test("runTransaction failure > on rollback") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnRollback = true)
    val zio: ZIO[Any, Either[DbException, String], Int] = cs.runTransaction(_ => ZIO.fail("Not a good query"))
    zio.cause.map {
      case Cause.Then(Cause.Fail(firstError, _), Cause.Fail(secondError, _)) =>
        assertTrue(
          firstError == Right("Not a good query"),
          secondError == Left(DbException.Wrapped(FailingConnectionSource.RollbackException))
        )
    }
  }

  private val testRunTransactionFailureOnClose = test("runTransaction failure > on close") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnClose = true)
    val zio: ZIO[Any, Either[DbException, Throwable], Int] = cs.runTransaction(connectionCountQuery)
    zio.cause.map {
      case Cause.Die(ex, _) => assertTrue(ex == DbException.Wrapped(FailingConnectionSource.CloseException))
    }
  }

  private val testRunAutoCommitFailureOnOpen = test("runAutoCommit failure > on open") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnOpen = true)
    val zio: ZIO[Any, Either[DbException, Throwable], Int] = cs.runAutoCommit(connectionCountQuery)
    zio.flip.map { e =>
      assertTrue(e == Left(DbException.Wrapped(FailingConnectionSource.OpenException)))
    }
  }

  private val testRunAutoCommitFailureOnAutoCommit = test("runAutoCommit failure > on auto-commit") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnAutoCommit = true)
    val zio: ZIO[Any, Either[DbException, Throwable], Int] = cs.runAutoCommit(connectionCountQuery)
    zio.flip.map { e =>
      assertTrue(e == Left(DbException.Wrapped(FailingConnectionSource.AutoCommitException)))
    }
  }

  private val testRunAutoCommitFailureOnClose = test("runAutoCommit failure > on close") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnClose = true)
    val zio: ZIO[Any, Either[DbException, Throwable], Int] = cs.runAutoCommit(connectionCountQuery)
    zio.cause.map {
      case Cause.Die(ex, _) => assertTrue(ex == DbException.Wrapped(FailingConnectionSource.CloseException))
    }
  }

  private val testRunTransactionStreamFailureOnOpen = test("runTransactionOrDieStream failure > on open") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnOpen = true)
    val zio: ZStream[Any, Throwable, Int] = cs.runTransactionOrDieStream(connectionCountQueryStream)
    zio.runCollect.cause.map {
      case Cause.Die(ex, _) => assertTrue(ex == DbException.Wrapped(FailingConnectionSource.OpenException))
    }
  }

  private val testRunTransactionStreamFailureOnAutoCommit = test("runTransactionOrDieStream failure > on auto-commit") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnAutoCommit = true)
    val zio: ZStream[Any, Throwable, Int] = cs.runTransactionOrDieStream(connectionCountQueryStream)
    zio.runCollect.cause.map {
      case Cause.Die(ex, _) => assertTrue(ex == DbException.Wrapped(FailingConnectionSource.AutoCommitException))
    }
  }

  private val testRunTransactionStreamFailureOnCommit = test("runTransactionOrDieStream failure > on commit (after success)") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnCommit = true)
    val zio: ZStream[Any, Throwable, Int] = cs.runTransactionOrDieStream(connectionCountQueryStream)
    zio.runCollect.cause.map {
      case Cause.Die(ex, _) => assertTrue(ex == DbException.Wrapped(FailingConnectionSource.CommitException))
    }
  }

  private val testRunTransactionStreamFailureOnCommitAfterFailure = test("runTransactionOrDieStream failure > on commit (after failure)") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnCommit = true)
    val zio: ZStream[Any, Exception, Int] = cs.runTransactionOrDieStream(_ => ZStream.fail(TestException("Not a good query")), commitOnFailure = true)
    zio.runCollect.cause.map {
      case Cause.Then(Cause.Fail(firstError, _), Cause.Die(secondError, _)) =>
        assertTrue(
          firstError == TestException("Not a good query"),
          secondError == DbException.Wrapped(FailingConnectionSource.CommitException)
        )
    }
  }

  private val testRunTransactionStreamFailureOnRollback = test("runTransactionOrDieStream failure > on rollback") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnRollback = true)
    val zio: ZStream[Any, Exception, Int] = cs.runTransactionOrDieStream(_ => ZStream.fail(TestException("Not a good query")))
    zio.runCollect.cause.map {
      case Cause.Then(Cause.Fail(firstError, _), Cause.Die(secondError, _)) =>
        assertTrue(
          firstError == TestException("Not a good query"),
          secondError == DbException.Wrapped(FailingConnectionSource.RollbackException)
        )
    }
  }

  private val testRunTransactionStreamFailureOnClose = test("runTransactionOrDieStream failure > on close") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnClose = true)
    val zio: ZStream[Any, Throwable, Int] = cs.runTransactionOrDieStream(connectionCountQueryStream)
    zio.runCollect.cause.map {
      case Cause.Die(ex, _) => assertTrue(ex == DbException.Wrapped(FailingConnectionSource.CloseException))
    }
  }

  private val testRunAutoCommitStreamFailureOnOpen = test("runAutoCommitStream failure > on open") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnOpen = true)
    val zio: ZStream[Any, Either[DbException, Throwable], Int] = cs.runAutoCommitStream(connectionCountQueryStream)
    zio.runCollect.flip.map { e =>
      assertTrue(e == Left(DbException.Wrapped(FailingConnectionSource.OpenException)))
    }
  }

  private val testRunAutoCommitStreamFailureOnAutoCommit = test("runAutoCommitStream failure > on auto-commit") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnAutoCommit = true)
    val zio: ZStream[Any, Either[DbException, Throwable], Int] = cs.runAutoCommitStream(connectionCountQueryStream)
    zio.runCollect.flip.map { e =>
      assertTrue(e == Left(DbException.Wrapped(FailingConnectionSource.AutoCommitException)))
    }
  }

  private val testRunAutoCommitStreamFailureOnClose = test("runAutoCommitStream failure > on close") {
    val cs = new FailingConnectionSource(errorStrategies)(failOnClose = true)
    val zio: ZStream[Any, Either[DbException, Throwable], Int] = cs.runAutoCommitStream(connectionCountQueryStream)
    zio.runCollect.cause.map {
      case Cause.Die(ex, _) => assertTrue(ex == DbException.Wrapped(FailingConnectionSource.CloseException))
    }
  }

  case class TestException(content: String) extends Exception(content)
}
