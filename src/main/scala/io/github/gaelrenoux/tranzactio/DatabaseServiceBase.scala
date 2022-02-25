package io.github.gaelrenoux.tranzactio

import zio.{Tag, ZIO}

import java.sql.{Connection => JdbcConnection}


/** Template implementing a default transactional mechanism, based on a ConnectionSource. */
abstract class DatabaseServiceBase[Connection: Tag](connectionSource: ConnectionSource.Service)
  extends DatabaseOps.ServiceOps[Connection] {

  import connectionSource._

  def connectionFromJdbc(connection: JdbcConnection): ZIO[Any, Nothing, Connection]

  override def transactionR[R, E, A](zio: ZIO[Connection with R, E, A], commitOnFailure: Boolean = false)
    (implicit errorStrategies: ErrorStrategiesRef): ZIO[R, Either[DbException, E], A] =
    ZIO.environmentWithZIO[R] { r =>
      runTransaction({ c: JdbcConnection =>
        connectionFromJdbc(c).map(r ++ _).flatMap(zio.provide(_))
      }, commitOnFailure)
    }

  override def autoCommitR[R, E, A](zio: ZIO[Connection with R, E, A])
    (implicit errorStrategies: ErrorStrategiesRef): ZIO[R, Either[DbException, E], A] = {
    ZIO.environmentWithZIO[R] { r =>
      runAutoCommit { c: JdbcConnection =>
        connectionFromJdbc(c).map(r ++ _).flatMap(zio.provide(_))
      }
    }
  }

}

