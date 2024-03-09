package io.github.gaelrenoux.tranzactio

import io.github.gaelrenoux.tranzactio.test.{DatabaseModuleTestOps, NoopJdbcConnection}
import zio.ZIO.attemptBlocking
import zio.{Tag, Trace, ZIO, ZLayer}

import java.sql.{Connection => JdbcConnection}

/** TranzactIO module for Anorm. Note that the 'Connection' also includes the Blocking module, as tzio also needs to
 * provide the wrapper around the synchronous Anorm method. */
package object anorm extends Wrapper {
  override final type Connection = JdbcConnection
  override final type Database = Database.Service
  override final type DbContext = EmptyDbContext.type
  override final type Query[A] = JdbcConnection => A
  override final type TranzactIO[A] = ZIO[Connection, DbException, A]

  private[tranzactio] val connectionTag = implicitly[Tag[Connection]]

  override final def tzio[A](q: => Query[A])(implicit trace: Trace): TranzactIO[A] =
    ZIO.serviceWithZIO[Connection] { c =>
      attemptBlocking(q(c))
    }.mapError(DbException.Wrapped.apply)

  /** Database for the Anorm wrapper */
  object Database
    extends DatabaseModuleBase[Connection, DatabaseOps.ServiceOps[Connection], DbContext]
      with DatabaseModuleTestOps[Connection, DbContext] {
    self =>

    private[tranzactio] override implicit val connectionTag: Tag[Connection] = anorm.connectionTag

    override def noConnection(implicit trace: Trace): ZIO[Any, Nothing, Connection] = ZIO.succeed(NoopJdbcConnection)

    /** Creates a Database Layer which requires an existing ConnectionSource. */
    override final def fromConnectionSource(implicit dbContext: DbContext, trace: Trace): ZLayer[ConnectionSource, Nothing, Database] =
      ZLayer.fromFunction { (cs: ConnectionSource) => new DatabaseService(cs) }
  }

  private class DatabaseService(cs: ConnectionSource) extends DatabaseServiceBase[Connection](cs) {
    override final def connectionFromJdbc(connection: => JdbcConnection)(implicit trace: Trace): ZIO[Any, Nothing, Connection] =
      ZIO.succeed(connection)
  }

  override final type DatabaseT[M] = DatabaseTBase[M, Connection]

  object DatabaseT extends DatabaseTBase.Companion[Connection, DbContext] {
    def apply[M: Tag]: Module[M] = new Module[M](Database)
  }
}
