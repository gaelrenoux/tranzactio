package io.github.gaelrenoux.tranzactio

import java.sql.Connection

import javax.sql.DataSource
import zio._
import zio.blocking._
import zio.clock.Clock

/** A module able to provide and manage connections. They typically come from a connection pool. */
object ConnectionSource {

  trait Service {

    def runTransaction[R, E, A](task: Connection => ZIO[R, E, A], commitOnFailure: Boolean = false)
      (implicit errorStrategies: ErrorStrategiesRef): ZIO[R, Either[DbException, E], A]

    def runAutoCommit[R, E, A](task: Connection => ZIO[R, E, A])
      (implicit errorStrategies: ErrorStrategiesRef): ZIO[R, Either[DbException, E], A]
  }

  /** ConnectionSource with standard behavior. Children class need to implement `getConnection`. */
  abstract class ServiceBase(
      env: Blocking with Clock,
      val defaultErrorStrategies: ErrorStrategiesRef
  ) extends ConnectionSource.Service {

    /** Main function: how to obtain a connection. Needs to be provided. */
    protected def getConnection: RIO[Blocking, Connection]

    def runTransaction[R, E, A](task: Connection => ZIO[R, E, A], commitOnFailure: Boolean = false)
      (implicit errorStrategies: ErrorStrategiesRef): ZIO[R, Either[DbException, E], A] =
      openConnection.mapError(Left(_)).bracket(closeConnection(_).orDie) { c: Connection =>
        setAutoCommit(c, autoCommit = false)
          .mapError(Left(_))
          .zipRight {
            task(c).mapError(Right(_))
          }
          .tapBoth(
            _ => if (commitOnFailure) commitConnection(c).mapError(Left(_)) else rollbackConnection(c).mapError(Left(_)),
            _ => commitConnection(c).mapError(Left(_))
          )
      }

    def runAutoCommit[R, E, A](task: Connection => ZIO[R, E, A])
      (implicit errorStrategies: ErrorStrategiesRef): ZIO[R, Either[DbException, E], A] =
      openConnection.mapError(Left(_)).bracket(closeConnection(_).orDie) { c: Connection =>
        setAutoCommit(c, autoCommit = true)
          .mapError(Left(_))
          .zipRight {
            task(c).mapError(Right(_))
          }
      }

    // TODO handle error reporting when retrying

    private def bottomErrorStrategy(implicit errorStrategies: ErrorStrategiesRef) =
      errorStrategies.orElse(defaultErrorStrategies).orElseDefault

    def openConnection(implicit errorStrategies: ErrorStrategiesRef): ZIO[Any, DbException, Connection] =
      wrap(bottomErrorStrategy.openConnection) {
        getConnection.mapError(e => DbException.Wrapped(e))
      }

    def setAutoCommit(c: Connection, autoCommit: Boolean)(implicit errorStrategies: ErrorStrategiesRef): ZIO[Any, DbException, Unit] =
      wrap(bottomErrorStrategy.setAutoCommit) {
        effectBlocking(c.setAutoCommit(autoCommit))
      }

    def commitConnection(c: Connection)(implicit errorStrategies: ErrorStrategiesRef): ZIO[Any, DbException, Unit] =
      wrap(bottomErrorStrategy.commitConnection) {
        effectBlocking(c.commit())
      }

    def rollbackConnection(c: Connection)(implicit errorStrategies: ErrorStrategiesRef): ZIO[Any, DbException, Unit] =
      wrap(bottomErrorStrategy.rollbackConnection) {
        effectBlocking(c.rollback())
      }

    /** Cannot fail */
    def closeConnection(c: Connection)(implicit errorStrategies: ErrorStrategiesRef): ZIO[Any, DbException, Unit] =
      wrap(bottomErrorStrategy.closeConnection) {
        effectBlocking(c.close())
      }

    private def wrap[R, A](es: ErrorStrategy)(z: ZIO[Blocking, Throwable, A]) = es {
      z.mapError(e => DbException.Wrapped(e))
    }.provide(env)

  }

  /** Service based on a DataSource. */
  private class DatasourceService(
      env: Has[DataSource] with Blocking with Clock,
      defaultErrorStrategies: ErrorStrategiesRef
  ) extends ServiceBase(env, defaultErrorStrategies) {
    private val ds = env.get[DataSource]

    override def getConnection: RIO[Blocking, Connection] = effectBlocking {
      ds.getConnection()
    }
  }

  val any: ZLayer[DataSource, Nothing, DataSource] = ZLayer.requires[DataSource]

  /** ConnectionSource created from a DataSource. Any connection pool you use should be able to provide a DataSource.
   * When no implicit ErrorStrategies is available, the default ErrorStrategies will be used.
   */
  val fromDatasource: ZLayer[Has[DataSource] with Blocking with Clock, Nothing, ConnectionSource] =
    ZIO.access[Has[DataSource] with Blocking with Clock](new DatasourceService(_, ErrorStrategies.Parent)).toLayer

  /** ConnectionSource created from a DataSource. Any connection pool you use should be able to provide a DataSource.
   * When no implicit ErrorStrategies is available, the ErrorStrategies provided in the layer will be used.
   */
  val fromDatasourceAndErrorStrategies: ZLayer[Has[DataSource] with Has[ErrorStrategiesRef] with Blocking with Clock, Nothing, ConnectionSource] =
    ZIO.access[Has[DataSource] with Has[ErrorStrategiesRef] with Blocking with Clock] { env =>
      val errorStrategies = env.get[ErrorStrategiesRef]
      new DatasourceService(env, errorStrategies)
    }.toLayer
}
