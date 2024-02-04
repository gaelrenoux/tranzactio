package io.github.gaelrenoux.tranzactio

import zio.{Task, Trace, ZIO}

import java.sql.{Connection, DriverManager}
import java.util.UUID

/** A ConnectionSource that fails on some operations */
class FailingConnectionSource(defaultErrorStrategies: ErrorStrategiesRef)(
    failOnOpen: Boolean = false,
    failOnAutoCommit: Boolean = false,
    failOnCommit: Boolean = false,
    failOnRollback: Boolean = false,
    failOnClose: Boolean = false
) extends ConnectionSource.ServiceBase(defaultErrorStrategies) {

  import FailingConnectionSource._

  override protected def getConnection(implicit trace: Trace): Task[Connection] = ZIO.attemptBlocking {
    val uuid = UUID.randomUUID().toString
    DriverManager.getConnection(s"jdbc:h2:mem:$uuid;DB_CLOSE_DELAY=10", "sa", "sa")
  }

  override def openConnection(implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[Any, DbException, Connection] =
    if (failOnOpen) ZIO.fail(DbException.Wrapped(OpenException))
    else super.openConnection

  override def setAutoCommit(c: => Connection, autoCommit: => Boolean)(implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[Any, DbException, Unit] = {
    if (failOnAutoCommit) ZIO.fail(DbException.Wrapped(AutoCommitException))
    else super.setAutoCommit(c, autoCommit)
  }

  override def commitConnection(c: => Connection)(implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[Any, DbException, Unit] =
    if (failOnCommit) ZIO.fail(DbException.Wrapped(CommitException))
    else super.commitConnection(c)

  override def rollbackConnection(c: => Connection)(implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[Any, DbException, Unit] =
    if (failOnRollback) ZIO.fail(DbException.Wrapped(RollbackException))
    else super.rollbackConnection(c)

  override def closeConnection(c: => Connection)(implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[Any, DbException, Unit] =
    if (failOnClose) ZIO.fail(DbException.Wrapped(CloseException))
    else super.closeConnection(c)
}

object FailingConnectionSource {
  case object OpenException extends RuntimeException("Oops my connection")

  case object AutoCommitException extends RuntimeException("Oops my auto-commit")

  case object CommitException extends RuntimeException("Oops my commit")

  case object RollbackException extends RuntimeException("Oops my rollback")

  case object CloseException extends RuntimeException("Oops my closing")
}
