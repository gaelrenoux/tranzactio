package io.github.gaelrenoux.tranzactio.doobie

import java.sql.{Connection => SqlConnection}

import io.github.gaelrenoux.tranzactio._
import javax.sql.DataSource
import zio._
import zio.blocking.Blocking
import zio.clock.Clock

/** A Database wrapping Doobie. */
object DoobieDatabase {

  type Service = DatabaseServiceApi[Connection]

  def fromConnectionSource: ZLayer[ConnectionSource with Blocking, Nothing, Database] =
    ZLayer.fromFunction { env: ConnectionSource with Blocking =>
      new DatabaseServiceBase[Connection](env.get[ConnectionSource.Service]) with DoobieDatabase.Service {
        override def connectionFromSql(connection: SqlConnection): ZIO[Any, Nothing, Connection] =
          Connection.fromSqlConnection(connection).provide(env)
      }
    }

  def fromDriverManager(
      url: String, user: String, password: String,
      driver: Option[String] = None,
      errorStrategies: ErrorStrategies = ErrorStrategies.Default
  ): ZLayer[Blocking with Clock, Nothing, Database] =
    (ConnectionSource.fromDriverManager(url, user, password, driver, errorStrategies) ++ Blocking.any) >>> fromConnectionSource

  def fromDatasource(
      datasource: DataSource,
      errorStrategies: ErrorStrategies = ErrorStrategies.Default
  ): ZLayer[Blocking with Clock, Nothing, Database] =
    (ConnectionSource.fromDatasource(datasource, errorStrategies) ++ Blocking.any) >>> fromConnectionSource



  /* Commodity methods */

  def transactionR[R <: Has[_], E, A](zio: ZIO[R with Connection, E, A]): ZIO[Database with R, Either[DbException, E], A] =
    ZIO.accessM { db: Database => db.get.transactionR[R, E, A](zio) }

  def transaction[E, A](zio: ZIO[Connection, E, A]): ZIO[Database, Either[DbException, E], A] =
    ZIO.accessM { db: Database => db.get.transaction[E, A](zio) }

  def transactionOrWidenR[R <: Has[_], E >: DbException, A](zio: ZIO[R with Connection, E, A]): ZIO[Database with R, E, A] =
    ZIO.accessM { db: Database => db.get.transactionOrWidenR[R, E, A](zio) }

  def transactionOrWiden[E >: DbException, A](zio: ZIO[Connection, E, A]): ZIO[Database, E, A] =
    ZIO.accessM { db: Database => db.get.transactionOrWiden[E, A](zio) }

  def transactionOrDieR[R <: Has[_], E, A](zio: ZIO[R with Connection, E, A]): ZIO[Database with R, E, A] =
    ZIO.accessM { db: Database => db.get.transactionOrDieR[R, E, A](zio) }

  def transactionOrDie[E >: DbException, A](zio: ZIO[Connection, E, A]): ZIO[Database, E, A] =
    ZIO.accessM { db: Database => db.get.transactionOrDie[E, A](zio) }

  def autoCommitR[R <: Has[_], E, A](zio: ZIO[R with Connection, E, A]): ZIO[Database with R, Either[DbException, E], A] =
    ZIO.accessM { db: Database => db.get.autoCommitR[R, E, A](zio) }

  def autoCommit[E, A](zio: ZIO[Connection, E, A]): ZIO[Database, Either[DbException, E], A] =
    ZIO.accessM { db: Database => db.get.autoCommit[E, A](zio) }

  def autoCommitOrWidenR[R <: Has[_], E >: DbException, A](zio: ZIO[R with Connection, E, A]): ZIO[Database with R, E, A] =
    ZIO.accessM { db: Database => db.get.autoCommitOrWidenR[R, E, A](zio) }

  def autoCommitOrWiden[E >: DbException, A](zio: ZIO[Connection, E, A]): ZIO[Database, E, A] =
    ZIO.accessM { db: Database => db.get.autoCommitOrWiden[E, A](zio) }

  def autoCommitOrDieR[R <: Has[_], E, A](zio: ZIO[R with Connection, E, A]): ZIO[Database with R, E, A] =
    ZIO.accessM { db: Database => db.get.autoCommitOrDieR[R, E, A](zio) }

  def autoCommitOrDie[E >: DbException, A](zio: ZIO[Connection, E, A]): ZIO[Database, E, A] =
    ZIO.accessM { db: Database => db.get.autoCommitOrDie[E, A](zio) }

}

