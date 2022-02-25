package io.github.gaelrenoux.tranzactio


import zio.Clock
import zio.{Tag, ZIO, ZLayer}

import java.sql.{Connection => JdbcConnection}
import javax.sql.DataSource

/** Template implementing the commodity methods for a Db module. */
abstract class DatabaseModuleBase[Connection, Dbs <: DatabaseOps.ServiceOps[Connection] : Tag]
  extends DatabaseOps.ModuleOps[Connection, Dbs] {

  type Database = Dbs
  type Service = DatabaseOps.ServiceOps[Connection]

  override def transactionR[R <: *, E, A](
      zio: ZIO[Connection with R, E, A],
      commitOnFailure: Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[Dbs with R, Either[DbException, E], A] = {
    ZIO.environmentWithZIO { db: Dbs =>
      db.get[Dbs].transactionR[R, E, A](zio, commitOnFailure)
    }
  }

  override def autoCommitR[R <: *, E, A](
      zio: ZIO[Connection with R, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[Dbs with R, Either[DbException, E], A] = {
    ZIO.environmentWithZIO { db: Dbs =>
      db.get[Dbs].autoCommitR[R, E, A](zio).provideSome[R] { r =>
        val env = r ++ Has(()) // needed for the compiler
        env
      }
    }
  }

  /** Creates a Database Layer which requires an existing ConnectionSource. */
  def fromConnectionSource: ZLayer[ConnectionSource with TranzactioEnv, Nothing, Database]

  /** Creates a Tranzactio Connection, given a JDBC connection and a Blocking. Useful for some utilities. */
  def connectionFromJdbc(env: TranzactioEnv, connection: JdbcConnection): ZIO[Any, Nothing, Connection]

  /** Commodity method: creates a Database Layer which includes its own ConnectionSource based on a DataSource. Most
   * connection pool implementations should be able to provide you a DataSource.
   *
   * When no implicit ErrorStrategies is available, the default ErrorStrategies will be used.
   */
  final val fromDatasource: ZLayer[DataSource with TranzactioEnv, Nothing, Database] =
    (ConnectionSource.fromDatasource ++ Blocking.any ++ Clock.any) >>> fromConnectionSource

  /** As `fromDatasource`, but provides a default ErrorStrategiesRef. When a method is called with no available implicit
   * ErrorStrategiesRef, the ErrorStrategiesRef in argument will be used. */
  final def fromDatasource(errorStrategies: ErrorStrategiesRef): ZLayer[DataSource with Any with Clock, Nothing, Database] =
    (ConnectionSource.fromDatasource(errorStrategies) ++ Blocking.any ++ Clock.any) >>> fromConnectionSource

  /** As `fromDatasource(ErrorStrategiesRef)`, but an `ErrorStrategies` is provided through a layer instead of as a parameter. */
  final val fromDatasourceAndErrorStrategies: ZLayer[DataSource with ErrorStrategies with Any with Clock, Nothing, Database] =
    (ConnectionSource.fromDatasourceAndErrorStrategies ++ Blocking.any ++ Clock.any) >>> fromConnectionSource


  val any: ZLayer[Database, Nothing, Database] = ZLayer.requires[Database]
}
