package io.github.gaelrenoux.tranzactio

import zio.stream.ZStream
import zio.{Tag, Trace, ZEnvironment, ZIO}

import java.sql.{Connection => JdbcConnection}


/** Template implementing a default transactional mechanism, based on a ConnectionSource. */
abstract class DatabaseServiceBase[Connection: Tag](connectionSource: ConnectionSource.Service)
  extends DatabaseOps.ServiceOps[Connection] {

  import connectionSource._

  def connectionFromJdbc(connection: => JdbcConnection)(implicit trace: Trace): ZIO[Any, Nothing, Connection]

  override def transaction[R, E, A](zio: => ZIO[Connection with R, E, A], commitOnFailure: => Boolean = false)
    (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R, Either[DbException, E], A] =
    runTransaction({ (c: JdbcConnection) =>
      connectionFromJdbc(c)
        .flatMap { connection => zio.provideSomeEnvironment[R](_ ++ ZEnvironment(connection)) }
    }, commitOnFailure)

  override def transactionOrDieStream[R, E, A](stream: => ZStream[Connection with R, E, A], commitOnFailure: => Boolean = false)
    (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZStream[R, E, A] =
    runTransactionOrDieStream({ (c: JdbcConnection) =>
      ZStream
        .fromZIO(connectionFromJdbc(c))
        .flatMap { connection => stream.provideSomeEnvironment[R](_ ++ ZEnvironment(connection)) }
    }, commitOnFailure)

  override def autoCommit[R, E, A](zio: => ZIO[Connection with R, E, A])
    (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R, Either[DbException, E], A] =
    runAutoCommit { (c: JdbcConnection) =>
      connectionFromJdbc(c)
        .flatMap { connection => zio.provideSomeEnvironment[R](_ ++ ZEnvironment(connection)) }
    }

  override def autoCommitStream[R, E, A](stream: => ZStream[Connection with R, E, A])
    (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZStream[R, Either[DbException, E], A] =
    runAutoCommitStream { (c: JdbcConnection) =>
      ZStream
        .fromZIO(connectionFromJdbc(c))
        .flatMap { connection => stream.provideSomeEnvironment[R](_ ++ ZEnvironment(connection)) }
    }

}

