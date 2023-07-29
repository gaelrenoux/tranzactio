package io.github.gaelrenoux.tranzactio

import zio.{Tag, ZEnvironment, ZIO, Trace}

import java.sql.{Connection => JdbcConnection}


/** Template implementing a default transactional mechanism, based on a ConnectionSource. */
abstract class DatabaseServiceBase[Connection: Tag](connectionSource: ConnectionSource.Service)
  extends DatabaseOps.ServiceOps[Connection] {

  import connectionSource._

  def connectionFromJdbc(connection: => JdbcConnection)(implicit trace: Trace): ZIO[Any, Nothing, Connection]

  override def transaction[R, E, A](zio: => ZIO[Connection with R, E, A], commitOnFailure: => Boolean = false)
    (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R, Either[DbException, E], A] =
    ZIO.environmentWithZIO[R] { r =>
      runTransaction({ (c: JdbcConnection) =>
        connectionFromJdbc(c)
          .map(r ++ ZEnvironment(_))
          .flatMap(zio.provideEnvironment(_))
      }, commitOnFailure)
    }

  override def autoCommit[R, E, A](zio: => ZIO[Connection with R, E, A])
    (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R, Either[DbException, E], A] =
    ZIO.environmentWithZIO[R] { r =>
      runAutoCommit { (c: JdbcConnection) =>
        connectionFromJdbc(c)
          .map(r ++ ZEnvironment(_))
          .flatMap(zio.provideEnvironment(_))
      }
    }

}

