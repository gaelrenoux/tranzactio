package io.github.gaelrenoux.tranzactio

import java.sql.{Connection => JavaSqlConnection}

import zio.{Has, Tagged, ZIO}


/** Template implementing a default transactional mechanism, based on a ConnectionSource. */
abstract class DatabaseServiceBase[Connection <: Has[_] : Tagged](
    connectionSource: ConnectionSource.Service
) extends DatabaseServiceApi[Connection] {

  import connectionSource._

  def connectionFromSql(connection: JavaSqlConnection): ZIO[Any, Nothing, Connection]

  override def transactionR[R <: Has[_], E, A](zio: ZIO[R with Connection, E, A]): ZIO[R, Either[DbException, E], A] = {
    for {
      r <- ZIO.environment[R]
      a <- openConnection.mapError(Left(_)).bracket(closeConnection(_).orDie) { c: JavaSqlConnection =>
        setAutoCommit(c, autoCommit = false).mapError(Left(_))
          .as(c)
          .flatMap(connectionFromSql)
          .flatMap { c =>
            val env = r ++ c
            zio.mapError(Right(_)).provide(env)
          }
          .tapBoth(
            _ => rollbackConnection(c).mapError(Left(_)),
            _ => commitConnection(c).mapError(Left(_))
          )
      }
    } yield a
  }

  override def autoCommitR[R <: Has[_], E, A](zio: ZIO[R with Connection, E, A]): ZIO[R, Either[DbException, E], A] =
    for {
      r <- ZIO.environment[R]
      a <- openConnection.mapError(Left(_)).bracket(closeConnection(_).orDie) { c: JavaSqlConnection =>
        setAutoCommit(c, autoCommit = true).mapError(Left(_))
          .as(c)
          .flatMap(connectionFromSql)
          .flatMap { c =>
            val env = r ++ c
            zio.mapError(Right(_)).provide(env)
          }
      }
    } yield a
}

