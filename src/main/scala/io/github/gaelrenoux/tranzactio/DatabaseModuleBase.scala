package io.github.gaelrenoux.tranzactio

import javax.sql.DataSource
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{Has, Tag, ZIO, ZLayer}

/** Template implementing the commodity methods for a Db module. */
abstract class DatabaseModuleBase[Connection, Dbs <: DatabaseOps.ServiceOps[Connection] : Tag]
  extends DatabaseOps.ModuleOps[Connection, Dbs] {

  type Database = Has[Dbs]

  private[tranzactio]
  override def transactionRFull[R <: Has[_], E, A](zio: ZIO[R with Connection, E, A]): ZIO[Has[Dbs] with R, Either[DbException, E], A] = {
    ZIO.accessM { db: Has[Dbs] =>
      db.get[Dbs].transactionRFull[R, E, A](zio).provideSome[R] { r =>
        val env = r ++ Has(()) // needed for the compiler
        env
      }
    }
  }

  private[tranzactio]
  override def autoCommitRFull[R <: Has[_], E, A](zio: ZIO[R with Connection, E, A]): ZIO[Has[Dbs] with R, Either[DbException, E], A] = {
    ZIO.accessM { db: Has[Dbs] =>
      db.get[Dbs].autoCommitRFull[R, E, A](zio).provideSome[R] { r =>
        val env = r ++ Has(()) // needed for the compiler
        env
      }
    }
  }

  /** Creates a Database Layer which requires an existing ConnectionSource. */
  def fromConnectionSource: ZLayer[ConnectionSource with Blocking, Nothing, Database]

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
