package io.github.gaelrenoux.tranzactio

import java.sql.{Connection => JdbcConnection}

import _root_.doobie.free.KleisliInterpreter
import _root_.doobie.util.transactor.{Strategy, Transactor}
import cats.effect.Resource
import io.github.gaelrenoux.tranzactio.utils.ZCatsBlocker
import zio.blocking.Blocking
import zio.interop.catz._
import zio.{Has, Task, ZIO, ZLayer}


/** TranzactIO module for Doobie. */
package object doobie extends Wrapper {
  override final type Connection = Has[Connection.Service]
  override final type Database = Has[Database.Service]
  override final type Query[A] = _root_.doobie.ConnectionIO[A]

  override final def tzio[A](q: Query[A]): TranzactIO[A] =
    ZIO.accessM[Connection](_.get.run(q)).mapError(DbException.Wrapped)

  /** Connection for the Doobie wrapper */
  object Connection {

    trait Service {
      def run[A](q: Query[A]): Task[A]
    }

    /** LiveConnection: based on a Doobie transactor. */
    class ServiceLive private[doobie](transactor: Transactor[Task]) extends Connection.Service {
      final def run[A](q: Query[A]): Task[A] = transactor.trans.apply(q)
    }

    /** Creates a LiveConnection from a java.sql.Connection, constructing the Doobie transactor. */
    final def fromJdbcConnection(connection: JdbcConnection): ZIO[Blocking, Nothing, Connection] = ZCatsBlocker.map { b =>
      val connect = (c: JdbcConnection) => Resource.pure[Task, JdbcConnection](c)
      val interp = KleisliInterpreter[Task](b).ConnectionInterpreter
      val doobieTransactor = Transactor(connection, connect, interp, Strategy.void)
      new Connection.ServiceLive(doobieTransactor)
    }.map(Has(_))

  }

  /** Database for the Doobie wrapper */
  object Database extends DatabaseModuleBase[Connection, DatabaseOps.ServiceOps[Connection]] {
    self =>

    type Service = DatabaseOps.ServiceOps[Connection]

    /** How to provide a Connection for the module, given a JDBC connection and some environment. */
    def connectionFromJdbc(env: ConnectionSource with Blocking, connection: JdbcConnection): ZIO[Any, Nothing, Connection] =
      Connection.fromJdbcConnection(connection).provide(env)

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
