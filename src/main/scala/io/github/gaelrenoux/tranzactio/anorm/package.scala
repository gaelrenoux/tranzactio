package io.github.gaelrenoux.tranzactio

import java.sql.{Connection => JdbcConnection}

import javax.sql.DataSource
import zio.blocking.{Blocking, effectBlocking}
import zio.clock.Clock
import zio.{Has, ZIO, ZLayer}


/** TranzactIO module for Anorm. Note that the 'Connection' also includes the Blocking module, as tzio also needs to
 * provide the wrapper around the synchronous Anorm method. */
package object anorm extends Wrapper {
  override final type Connection = Has[JdbcConnection] with Blocking
  override final type Database = Has[Database.Service]
  override final type Query[A] = JdbcConnection => A

  override final def tzio[A](q: Query[A]): TranzactIO[A] =
    ZIO.accessM[Connection] { c =>
      effectBlocking(q(c.get))
    }.mapError(DbException.Wrapped)

  /** Database for the Doobie wrapper */
  object Database extends DatabaseModuleBase[Connection, DatabaseOps.ServiceOps[Connection]] {

    type Service = DatabaseOps.ServiceOps[Connection]

    /** Creates a Database Layer which requires an existing ConnectionSource. */
    final def fromConnectionSource: ZLayer[ConnectionSource with Blocking, Nothing, Database] =
      ZLayer.fromFunction { env: ConnectionSource with Blocking =>
        new DatabaseServiceBase[Connection](env.get[ConnectionSource.Service]) with Database.Service {
          override final def connectionFromJdbc(connection: JdbcConnection): ZIO[Any, Nothing, Connection] =
            ZIO.succeed(Has(connection) ++ env)
        }
      }

    /** Commodity method: creates a Database Layer which includes its own ConnectionSource based on a DriverManager. You
     * should probably not use this method in production, as a new connection is created each time it is required. You
     * should use a connection pool, and create the Database Layer using `fromDatasource`. */
    final def fromDriverManager(
        url: String, user: String, password: String,
        driver: Option[String] = None,
        errorStrategies: ErrorStrategies = ErrorStrategies.Brutal
    ): ZLayer[Blocking with Clock, Nothing, Database] =
      (ConnectionSource.fromDriverManager(url, user, password, driver, errorStrategies) ++ Blocking.any) >>> fromConnectionSource

    /** Commodity method: creates a Database Layer which includes its own ConnectionSource based on a DataSource. Most
     * connection pool implementations should be able to provide you a DataSource. */
    final def fromDatasource(
        datasource: DataSource,
        errorStrategies: ErrorStrategies = ErrorStrategies.Brutal
    ): ZLayer[Blocking with Clock, Nothing, Database] =
      (ConnectionSource.fromDatasource(datasource, errorStrategies) ++ Blocking.any) >>> fromConnectionSource

  }


}
