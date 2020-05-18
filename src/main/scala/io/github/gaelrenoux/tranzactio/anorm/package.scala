package io.github.gaelrenoux.tranzactio

import java.sql.{Connection => JdbcConnection}

import zio.blocking.{Blocking, effectBlocking}
import zio.{Has, ZIO, ZLayer}


/** TranzactIO module for Anorm. Note that the 'Connection' also includes the Blocking module, as tzio also needs to
 * provide the wrapper around the synchronous Anorm method. */
package object anorm extends Wrapper {
  override final type Connection = Has[JdbcConnection] with Blocking
  override final type Database = Has[Database.Service]
  override final type Query[A] = JdbcConnection => A

  override final def tzio[A](q: Query[A]): TranzactIO[A] =
    ZIO.accessM[Connection] { c =>
      effectBlocking(q(c.get[JdbcConnection]))
    }.mapError(DbException.Wrapped)

  /** Database for the Doobie wrapper */
  object Database extends DatabaseModuleBase[Connection, DatabaseOps.ServiceOps[Connection]] {
    self =>

    type Service = DatabaseOps.ServiceOps[Connection]

    /** How to provide a Connection for the module, given a JDBC connection and some environment. */
    def connectionFromJdbc(env: ConnectionSource with Blocking, connection: JdbcConnection): ZIO[Any, Nothing, Connection] =
      ZIO.succeed(Has(connection) ++ env)

    /** Creates a Database Layer which requires an existing ConnectionSource. */
    final def fromConnectionSource: ZLayer[ConnectionSource with Blocking, Nothing, Database] =
      ZLayer.fromFunction { env: ConnectionSource with Blocking =>
        new DatabaseServiceBase[Connection](env.get[ConnectionSource.Service]) with Database.Service {
          override final def connectionFromJdbc(connection: JdbcConnection): ZIO[Any, Nothing, Connection] =
            self.connectionFromJdbc(env, connection)
        }
      }
  }


}
