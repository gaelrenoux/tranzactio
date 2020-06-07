package io.github.gaelrenoux.tranzactio

import java.sql.Connection

import javax.sql.DataSource
import zio.blocking._
import zio.clock.Clock
import zio.{Has, RIO, ZIO, ZLayer}

/** A module able to provide and manage connections. They typically come from a connection pool. */
object ConnectionSource {

  trait Service {
    def openConnection: ZIO[Any, DbException, Connection]

    def setAutoCommit(c: Connection, autoCommit: Boolean): ZIO[Any, DbException, Unit]

    def commitConnection(c: Connection): ZIO[Any, DbException, Unit]

    def rollbackConnection(c: Connection): ZIO[Any, DbException, Unit]

    def closeConnection(c: Connection): ZIO[Any, DbException, Unit]
  }

  /** ConnectionSource with standard behavior. Children class need to implement `getConnection`. */
  abstract class ServiceBase(
      env: Blocking with Clock,
      val errorStrategies: ErrorStrategies = ErrorStrategies.Brutal
  ) extends ConnectionSource.Service {

    /** Main function: how to obtain a connection. Needs to be provided. */
    def getConnection: RIO[Blocking, Connection]

    // TODO handle error reporting when retrying

    def openConnection: ZIO[Any, DbException, Connection] = wrap(errorStrategies.openConnection) {
      getConnection.mapError(e => DbException.Wrapped(e))
    }

    def setAutoCommit(c: Connection, autoCommit: Boolean): ZIO[Any, DbException, Unit] = wrap(errorStrategies.setAutoCommit) {
      effectBlocking(c.setAutoCommit(autoCommit))
    }

    def commitConnection(c: Connection): ZIO[Any, DbException, Unit] = wrap(errorStrategies.commitConnection) {
      effectBlocking(c.commit())
    }

    def rollbackConnection(c: Connection): ZIO[Any, DbException, Unit] = wrap(errorStrategies.rollbackConnection) {
      effectBlocking(c.rollback())
    }

    /** Cannot fail */
    def closeConnection(c: Connection): ZIO[Any, DbException, Unit] = wrap(errorStrategies.closeConnection) {
      effectBlocking(c.close())
    }

    private def wrap[R, A](es: ErrorStrategy)(z: ZIO[Blocking, Throwable, A]) = es {
      z.mapError(e => DbException.Wrapped(e))
    }.provide(env)

  }

  /** Service based on a DataSource. */
  private class DatasourceService(
      env: Has[DataSource] with Blocking with Clock,
      errorStrategies: ErrorStrategies = ErrorStrategies.Brutal
  ) extends ServiceBase(env, errorStrategies) {
    private val ds = env.get[DataSource]

    override def getConnection: RIO[Blocking, Connection] = effectBlocking {
      ds.getConnection()
    }
  }

  /** ConnectionSource created from a DataSource. Any connection pool you use should be able to provide a DataSource. */
  def fromDatasource(
      errorStrategies: ErrorStrategies = ErrorStrategies.Brutal
  ): ZLayer[Has[DataSource] with Blocking with Clock, Nothing, ConnectionSource] =
    ZIO.access[Has[DataSource] with Blocking with Clock](new DatasourceService(_, errorStrategies)).toLayer

}
