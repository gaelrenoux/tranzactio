package io.github.gaelrenoux.tranzactio

import java.sql.{Connection => JdbcConnection}

import zio.{Has, Tag, ZIO}


/** Template implementing a default transactional mechanism, based on a ConnectionSource. */
abstract class DatabaseServiceBase[Connection <: Has[_] : Tag](connectionSource: ConnectionSource.Service)
  extends DatabaseOps.ServiceOps[Connection] {

  import connectionSource._

  def connectionFromJdbc(connection: JdbcConnection): ZIO[Any, Nothing, Connection]

  private[tranzactio] override def transactionRFull[R <: Has[_], E, A](
      zio: ZIO[R with Connection, E, A],
      commitOnFailure: Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef): ZIO[R, Either[DbException, E], A] =
    for {
      r <- ZIO.environment[R]
      a <- openConnection.mapError(Left(_)).bracket(closeConnection(_).orDie) { c: JdbcConnection =>
        setAutoCommit(c, autoCommit = false)
          .bimap(Left(_), _ => c)
          .flatMap(connectionFromJdbc)
          .flatMap { c =>
            val env = r ++ c
            zio.mapError(Right(_)).provide(env)
          }
          .tapBoth(
            _ => if (commitOnFailure) commitConnection(c).mapError(Left(_)) else rollbackConnection(c).mapError(Left(_)),
            _ => commitConnection(c).mapError(Left(_))
          )
      }
    } yield a

  private[tranzactio] override def autoCommitRFull[R <: Has[_], E, A](
      zio: ZIO[R with Connection, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef): ZIO[R, Either[DbException, E], A] =
    for {
      r <- ZIO.environment[R]
      a <- openConnection.mapError(Left(_)).bracket(closeConnection(_).orDie) { c: JdbcConnection =>
        setAutoCommit(c, autoCommit = true)
          .bimap(Left(_), _ => c)
          .flatMap(connectionFromJdbc)
          .flatMap { c =>
            val env = r ++ c
            zio.mapError(Right(_)).provide(env)
          }
      }
    } yield a

}

