package io.github.gaelrenoux.tranzactio

import _root_.doobie.free.KleisliInterpreter
import _root_.doobie.util.transactor.{Strategy, Transactor}
import cats.effect.Resource
import io.github.gaelrenoux.tranzactio.test.DatabaseModuleTestOps
import zio.interop.catz._
import zio.stream.ZStream
import zio.stream.interop.fs2z._
import zio.{Tag, Task, ZIO, ZLayer}

import java.sql.{Connection => JdbcConnection}
import zio.ZEnvironment

/** TranzactIO module for Doobie. */
package object doobie extends Wrapper {
  override final type Connection = Transactor[Task]
  override final type Database = Database.Service
  override final type Query[A] = _root_.doobie.ConnectionIO[A]
  override final type TranzactIO[A] = ZIO[Connection, DbException, A]
  final type TranzactIOStream[A] = ZStream[Connection, DbException, A]

  private[tranzactio] val connectionTag = implicitly[Tag[Connection]]

  /** Default queue size when converting from FS2 streams. */
  final val DefaultStreamQueueSize = 16

  override final def tzio[A](q: Query[A]): TranzactIO[A] =
    ZIO.environmentWithZIO[Connection] { c =>
      c.get.trans.apply(q)
    }.mapError(DbException.Wrapped)

  /** Converts a Doobie stream to a ZStream. Note that you can provide a queue size, default value is the same as in ZIO. */
  final def tzioStream[A](q: fs2.Stream[Query, A], queueSize: Int = DefaultStreamQueueSize): TranzactIOStream[A] =
    ZStream.environmentWithStream[Connection] { c =>
      c.get.transP.apply(q).toZStream(queueSize)
    }.mapError(DbException.Wrapped)

  /** Database for the Doobie wrapper */
  object Database
    extends DatabaseModuleBase[Connection, DatabaseOps.ServiceOps[Connection]]
      with DatabaseModuleTestOps[Connection] {
    self =>

    private[tranzactio] override implicit val connectionTag: Tag[Connection] = doobie.connectionTag

    /** How to provide a Connection for the module, given a JDBC connection and some environment. */
    final def connectionFromJdbc(env: TranzactioEnv, connection: JdbcConnection): ZIO[Any, Nothing, Connection] = {
      ZIO.runtime[TranzactioEnv].flatMap { implicit r: zio.Runtime[TranzactioEnv] =>
        ZIO.succeed[Connection] {
          val connect = (c: JdbcConnection) => Resource.pure[Task, JdbcConnection](c)
          val interp = KleisliInterpreter[Task].ConnectionInterpreter
          val tran = Transactor(connection, connect, interp, Strategy.void)
          tran
        }
      }.provideService(env)
    }

    /** Creates a Database Layer which requires an existing ConnectionSource. */
    final def fromConnectionSource: ZLayer[ConnectionSource with TranzactioEnv, Nothing, Database] = 
      ZLayer.fromFunctionEnvironment { env: ZEnvironment[ConnectionSource with TranzactioEnv] =>
        ZEnvironment(new DatabaseServiceBase[Connection](env.get[ConnectionSource.Service]) {
          override final def connectionFromJdbc(connection: JdbcConnection): ZIO[Any, Nothing, Connection] =
            self.connectionFromJdbc(env.get[TranzactioEnv], connection)
        })
        
      }

  }


}
