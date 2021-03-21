package io.github.gaelrenoux.tranzactio

import java.sql.{Connection => JdbcConnection}

import zio.{Has, Tag, ZIO}


/** Template implementing a default transactional mechanism, based on a ConnectionSource. */
abstract class DatabaseServiceBase[Connection <: Has[_] : Tag](connectionSource: ConnectionSource.Service)
  extends DatabaseOps.ServiceOps[Connection] {

  import connectionSource._

  def connectionFromJdbc(connection: JdbcConnection): ZIO[Any, Nothing, Connection]

  override def transactionR[R <: Has[_], E, A](zio: ZIO[Connection with R, E, A], commitOnFailure: Boolean = false)
    (implicit errorStrategies: ErrorStrategiesRef): ZIO[R, Either[DbException, E], A] =
    ZIO.accessM[R] { r =>
      runTransaction({ c: JdbcConnection =>
        connectionFromJdbc(c).map(r ++ _).flatMap(zio.provide(_))
      }, commitOnFailure)
    }

  override def autoCommitR[R <: Has[_], E, A](zio: ZIO[Connection with R, E, A])
    (implicit errorStrategies: ErrorStrategiesRef): ZIO[R, Either[DbException, E], A] = {
    ZIO.accessM[R] { r =>
      runAutoCommit { c: JdbcConnection =>
        connectionFromJdbc(c).map(r ++ _).flatMap(zio.provide(_))
      }
    }
  }

}

