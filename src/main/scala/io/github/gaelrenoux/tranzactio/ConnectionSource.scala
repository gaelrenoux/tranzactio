package io.github.gaelrenoux.tranzactio

import java.sql.{Connection, DriverManager}

import javax.sql.DataSource
import zio.blocking._
import zio.clock.Clock
import zio.{RIO, ZIO, ZLayer}

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

  /** ConnectionSource layer from a Java DriverManager. Do not use in production, as it creates a new connection every
   * time one is needed. Use a connection pool and `fromDatasource` instead. */
  def fromDriverManager(
      url: String, user: String, password: String,
      driver: Option[String] = None,
      errorStrategies: ErrorStrategies = ErrorStrategies.Brutal
  ): ZLayer[Blocking with Clock, Nothing, ConnectionSource] =
    ZLayer.fromFunction { env: Blocking with Clock =>
      new ServiceBase(env, errorStrategies) {
        override def getConnection: RIO[Blocking, Connection] = effectBlocking {
          driver.foreach(Class.forName)
          DriverManager.getConnection(url, user, password)
        }
      }
    }

  /** ConnectionSource layer from a DataSource. Any connection pool you use should be able to provide one. */
  def fromDatasource(
      datasource: DataSource,
      errorStrategies: ErrorStrategies = ErrorStrategies.Brutal
  ): ZLayer[Blocking with Clock, Nothing, ConnectionSource] =
    ZLayer.fromFunction { env: Blocking with Clock =>
      new ServiceBase(env, errorStrategies) {
        override def getConnection: RIO[Blocking, Connection] = effectBlocking {
          datasource.getConnection()
        }
      }
    }

}
