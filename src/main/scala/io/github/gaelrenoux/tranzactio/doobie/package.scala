package io.github.gaelrenoux.tranzactio

import java.sql.{Connection => JdbcConnection}

import _root_.doobie.free.KleisliInterpreter
import _root_.doobie.util.transactor.{Strategy, Transactor}
import cats.effect.Resource
import io.github.gaelrenoux.tranzactio.utils.ZCatsBlocker
import javax.sql.DataSource
import zio.blocking.Blocking
import zio.clock.Clock
import zio.interop.catz._
import zio.{Has, Task, ZIO, ZLayer}


/** TranzactIO module for Doobie. */
package object doobie extends Wrapper {
  override final type Connection = Has[Connection.Service]
  override final type Database = Has[Database.Service]
  override final type Query[A] = _root_.doobie.ConnectionIO[A]

  override final def tzio[A](q: Query[A]): TranzactIO[A] = ZIO.accessM[Connection](_.get.run(q)).mapError(DbException.Wrapped)

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

    type Service = DatabaseOps.ServiceOps[Connection]

    /** Creates a Database Layer which requires an existing ConnectionSource. */
    final def fromConnectionSource: ZLayer[ConnectionSource with Blocking, Nothing, Database] =
      ZLayer.fromFunction { env: ConnectionSource with Blocking =>
        new DatabaseServiceBase[Connection](env.get[ConnectionSource.Service]) with Database.Service {
          override final def connectionFromSql(connection: JdbcConnection): ZIO[Any, Nothing, Connection] =
            Connection.fromJdbcConnection(connection).provide(env)
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
