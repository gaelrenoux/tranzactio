package io.github.gaelrenoux.tranzactio

import _root_.doobie.free.KleisliInterpreter
import _root_.doobie.LogHandler
import _root_.doobie.util.transactor.{Strategy, Transactor}
import cats.effect.Resource
import io.github.gaelrenoux.tranzactio.test.DatabaseModuleTestOps
import io.github.gaelrenoux.tranzactio.test.NoopJdbcConnection
import zio.interop.catz._
import zio.stream.ZStream
import zio.stream.interop.fs2z._
import zio.{Tag, Task, Trace, ZIO, ZLayer}

import java.sql.{Connection => JdbcConnection}

/** TranzactIO module for Doobie. */
package object doobie extends Wrapper {
  override final type Connection = Transactor[Task]
  override final type Database = Database.Service
  override final type Query[A] = _root_.doobie.ConnectionIO[A]
  override final type TranzactIO[A] = ZIO[Connection, DbException, A]
  final type TranzactIOStream[A] = ZStream[Connection, DbException, A]

  private[tranzactio] val connectionTag = implicitly[Tag[Connection]]

  /** Default queue size when converting from FS2 streams. Same default value as in FS2RIOStreamSyntax.toZStream. */
  private final val DefaultStreamQueueSize = 16

  /** How to provide a Connection for the module, given a JDBC connection and some environment. */
  private final def connectionFromJdbc(connection: => JdbcConnection, dbContext: DbContext)(implicit trace: Trace): ZIO[Any, Nothing, Connection] = {
    ZIO.succeed {
      val connect = (c: JdbcConnection) => Resource.pure[Task, JdbcConnection](c)
      val interp = KleisliInterpreter[Task](dbContext.logHandler).ConnectionInterpreter
      val tran = Transactor(connection, connect, interp, Strategy.void)
      tran
    }
  }

  override final def tzio[A](q: => Query[A])(implicit trace: Trace): TranzactIO[A] =
    ZIO.serviceWithZIO[Connection] { c =>
      c.trans.apply(q)
    }.mapError(DbException.Wrapped.apply)

  /** Converts a Doobie stream to a ZStream. Note that you can provide a queue size, default value is the same as in ZIO. */
  final def tzioStream[A](q: => fs2.Stream[Query, A], queueSize: => Int = DefaultStreamQueueSize)(implicit trace: Trace): TranzactIOStream[A] =
    ZStream.serviceWithStream[Connection] { c =>
      c.transP.apply(q).toZStream(queueSize)
    }.mapError(DbException.Wrapped.apply)

  /** Database for the Doobie wrapper */
  object Database
    extends DatabaseModuleBase[Connection, DatabaseOps.ServiceOps[Connection], DbContext]
      with DatabaseModuleTestOps[Connection, DbContext] {
    self =>

    private[tranzactio] override implicit val connectionTag: Tag[Connection] = doobie.connectionTag

    override def noConnection(implicit trace: Trace): ZIO[Any, Nothing, Transactor[Task]] =
      connectionFromJdbc(NoopJdbcConnection, DbContext.Default)

    /** Creates a Database Layer which requires an existing ConnectionSource. */
    override final def fromConnectionSource(implicit dbContext: DbContext, trace: Trace): ZLayer[ConnectionSource, Nothing, Database] =
      ZLayer.fromFunction { (cs: ConnectionSource) => new DatabaseService(cs, dbContext) }
  }

  private class DatabaseService(cs: ConnectionSource, val dbContext: DbContext) extends DatabaseServiceBase[Connection](cs) {
    override final def connectionFromJdbc(connection: => JdbcConnection)(implicit trace: Trace): ZIO[Any, Nothing, Transactor[Task]] =
      doobie.connectionFromJdbc(connection, dbContext)
  }

  case class DbContext(
      logHandler: LogHandler[Task]
  )

  object DbContext {
    implicit val Default: DbContext = DbContext(LogHandler.noop[Task])
  }

}
