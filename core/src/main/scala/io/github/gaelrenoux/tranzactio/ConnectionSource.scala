package io.github.gaelrenoux.tranzactio

import zio.ZIO.attemptBlocking
import zio._

import java.sql.Connection
import javax.sql.DataSource

/** A module able to provide and manage connections. They typically come from a connection pool. */
object ConnectionSource {

  trait Service {

    def runTransaction[R, E, A](task: Connection => ZIO[R, E, A], commitOnFailure: => Boolean = false)
      (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R, Either[DbException, E], A]

    def runAutoCommit[R, E, A](task: Connection => ZIO[R, E, A])
      (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R, Either[DbException, E], A]
  }

  /** ConnectionSource with standard behavior. Children class need to implement `getConnection`. */
  abstract class ServiceBase(
      val defaultErrorStrategies: ErrorStrategiesRef
  ) extends ConnectionSource.Service {

    /** Main function: how to obtain a connection. Needs to be provided. */
    protected def getConnection(implicit trace: Trace): Task[Connection]

    def runTransaction[R, E, A](task: Connection => ZIO[R, E, A], commitOnFailure: => Boolean = false)
      (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R, Either[DbException, E], A] = {
      ZIO.acquireReleaseWith(openConnection.mapError(Left(_)))(closeConnection(_).orDie) { (c: Connection) =>
        setAutoCommit(c, autoCommit = false)
          .mapError(Left(_))
          .zipRight(task(c).mapError(Right(_)))
          .tapBoth(
            _ => if (commitOnFailure) commitConnection(c).mapError(Left(_)) else rollbackConnection(c).mapError(Left(_)),
            _ => commitConnection(c).mapError(Left(_))
          )
      }
    }

    def runAutoCommit[R, E, A](task: Connection => ZIO[R, E, A])
      (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R, Either[DbException, E], A] =
      ZIO.acquireReleaseWith(openConnection.mapError(Left(_)))(closeConnection(_).orDie) { (c: Connection) =>
        setAutoCommit(c, autoCommit = true)
          .mapError(Left(_))
          .zipRight {
            task(c).mapError(Right(_))
          }
      }

    // TODO handle error reporting when retrying

    private def bottomErrorStrategy(implicit errorStrategies: ErrorStrategiesRef) =
      errorStrategies.orElse(defaultErrorStrategies).orElseDefault

    def openConnection(implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[Any, DbException, Connection] =
      wrap(bottomErrorStrategy.openConnection) {
        getConnection.mapError(e => DbException.Wrapped(e))
      }

    def setAutoCommit(c: => Connection, autoCommit: => Boolean)
      (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[Any, DbException, Unit] =
      wrap(bottomErrorStrategy.setAutoCommit) {
        attemptBlocking(c.setAutoCommit(autoCommit))
      }

    def commitConnection(c: => Connection)(implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[Any, DbException, Unit] =
      wrap(bottomErrorStrategy.commitConnection) {
        attemptBlocking(c.commit())
      }

    def rollbackConnection(c: => Connection)(implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[Any, DbException, Unit] =
      wrap(bottomErrorStrategy.rollbackConnection) {
        attemptBlocking(c.rollback())
      }

    /** Cannot fail */
    def closeConnection(c: => Connection)(implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[Any, DbException, Unit] =
      wrap(bottomErrorStrategy.closeConnection) {
        attemptBlocking(c.close())
      }

    private def wrap[R, A](es: ErrorStrategy)(z: => ZIO[Any, Throwable, A])(implicit trace: Trace) = es {
      z.mapError(e => DbException.Wrapped(e))
    }

  }

  /** Service based on a DataSource. */
  private class DatasourceService(
      dataSource: DataSource,
      defaultErrorStrategies: ErrorStrategiesRef
  ) extends ServiceBase(defaultErrorStrategies) {

    override def getConnection(implicit trace: Trace): RIO[Any, Connection] = attemptBlocking {
      dataSource.getConnection()
    }
  }

  /** Service based on a single connection, which is reused each time. Uses a Semaphore to make sure the connection
   * can't be used by concurrent operations. The connection will not be closed. */
  private class SingleConnectionService(
      connection: Connection,
      semaphore: Semaphore,
      defaultErrorStrategies: ErrorStrategiesRef
  ) extends ServiceBase(defaultErrorStrategies) {

    override def getConnection(implicit trace: Trace): UIO[Connection] = ZIO.succeed(connection)

    override def closeConnection(c: => Connection)(implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[Any, Nothing, Unit] = ZIO.unit

    override def runTransaction[R, E, A](task: Connection => ZIO[R, E, A], commitOnFailure: => Boolean)
      (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R, Either[DbException, E], A] =
      semaphore.withPermit {
        super.runTransaction(task, commitOnFailure)
      }

    override def runAutoCommit[R, E, A](task: Connection => ZIO[R, E, A])
      (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R, Either[DbException, E], A] =
      semaphore.withPermit {
        super.runAutoCommit(task)
      }
  }

  /** ConnectionSource created from a DataSource. Any connection pool you use should be able to provide a DataSource.
   *
   * When a Database method is called with no available implicit ErrorStrategiesRef, the default ErrorStrategiesRef will
   * be used. */
  def fromDatasource(implicit trace: Trace): ZLayer[DataSource, Nothing, ConnectionSource] =
    fromDatasource(ErrorStrategies.Parent)

  /** As `fromDatasource`, but provides a default ErrorStrategiesRef.
   *
   * When a Database method is called with no available implicit ErrorStrategiesRef, the ErrorStrategiesRef in argument
   * will be used. */
  def fromDatasource(errorStrategies: ErrorStrategiesRef)(implicit trace: Trace): ZLayer[DataSource, Nothing, ConnectionSource] = {
    ZLayer {
      for {
        source <- ZIO.service[DataSource]
      } yield new DatasourceService(source, errorStrategies)
    }
  }

  /** As `fromDatasource(ErrorStrategiesRef)`, but an `ErrorStrategies` is provided through a layer instead of as a parameter. */
  def fromDatasourceAndErrorStrategies(implicit trace: Trace): ZLayer[DataSource with ErrorStrategies, Nothing, ConnectionSource] = {
    ZLayer {
      for {
        source <- ZIO.service[DataSource]
        errorStrategies <- ZIO.service[ErrorStrategies]
      } yield new DatasourceService(source, errorStrategies)
    }
  }

  /** ConnectionSource created from a single connection. If several operations are launched concurrently, they will wait
   * for the connection to be available (see the Semaphore documentation for details).
   *
   * When a Database method is called with no available implicit ErrorStrategiesRef, the default ErrorStrategiesRef will
   * be used. */
  def fromConnection(implicit trace: Trace): ZLayer[Connection, Nothing, ConnectionSource] =
    fromConnection(ErrorStrategies.Parent)

  /** As `fromConnection`, but provides a default ErrorStrategiesRef.
   *
   * When a Database method is called with no available implicit ErrorStrategiesRef, the ErrorStrategiesRef in argument
   * will be used. */
  def fromConnection(errorStrategiesRef: ErrorStrategiesRef)(implicit trace: Trace): ZLayer[Connection, Nothing, ConnectionSource] = {
    ZLayer {
      for {
        connection <- ZIO.service[Connection]
        semaphore <- Semaphore.make(1)
      } yield new SingleConnectionService(connection, semaphore, errorStrategiesRef)
    }
  }
}
