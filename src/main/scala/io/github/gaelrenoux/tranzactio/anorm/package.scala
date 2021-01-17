package io.github.gaelrenoux.tranzactio

import java.sql.{Connection => JdbcConnection}

import io.github.gaelrenoux.tranzactio.test.DatabaseModuleTestOps
import izumi.reflect.Tag
import zio.blocking.{Blocking, effectBlocking}
import zio.{Has, ZIO, ZLayer}


/** TranzactIO module for Anorm. Note that the 'Connection' also includes the Blocking module, as tzio also needs to
 * provide the wrapper around the synchronous Anorm method. */
package object anorm extends Wrapper {
  override final type Connection = Has[JdbcConnection] with Blocking
  override final type Database = Has[Database.Service]
  override final type Query[A] = JdbcConnection => A
  override final type TranzactIO[A] = ZIO[Connection, DbException, A]

  private[tranzactio] val connectionTag = implicitly[Tag[Connection]]

  override final def tzio[A](q: Query[A]): TranzactIO[A] =
    ZIO.accessM[Connection] { c =>
      effectBlocking(q(c.get[JdbcConnection]))
    }.mapError(DbException.Wrapped)

  /** Database for the Anorm wrapper */
  object Database
    extends DatabaseModuleBase[Connection, DatabaseOps.ServiceOps[Connection]]
      with DatabaseModuleTestOps[Connection] {
    self =>

    private[tranzactio] override implicit val connectionTag: Tag[Connection] = anorm.connectionTag

    /** How to provide a Connection for the module, given a JDBC connection and some environment. */
    final def connectionFromJdbc(env: Blocking, connection: JdbcConnection): ZIO[Any, Nothing, Connection] =
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
