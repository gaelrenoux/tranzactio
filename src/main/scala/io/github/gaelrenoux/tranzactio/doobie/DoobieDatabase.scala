package io.github.gaelrenoux.tranzactio.doobie

import java.sql.{Connection => SqlConnection}

import io.github.gaelrenoux.tranzactio._
import javax.sql.DataSource
import zio._
import zio.blocking.Blocking
import zio.clock.Clock

/** A Database wrapping Doobie. */
object DoobieDatabase extends DatabaseModuleBase[Connection, DatabaseOps.ServiceOps[Connection]] {

  type Service = DatabaseOps.ServiceOps[Connection]

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

}

