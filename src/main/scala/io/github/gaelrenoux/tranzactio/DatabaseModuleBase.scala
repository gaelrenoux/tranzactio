package io.github.gaelrenoux.tranzactio


import zio.{Tag, ZIO, ZLayer, Trace}

import java.sql.{Connection => JdbcConnection}
import javax.sql.DataSource

/** Template implementing the commodity methods for a Db module. */
abstract class DatabaseModuleBase[Connection, Database <: DatabaseOps.ServiceOps[Connection] : Tag]
  extends DatabaseOps.ModuleOps[Connection, Database] {

  type Service = DatabaseOps.ServiceOps[Connection]

  override def transaction[R, E, A](
      zio: => ZIO[Connection with R, E, A],
      commitOnFailure: => Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZIO[Database with R, Either[DbException, E], A] = {
    ZIO.serviceWithZIO { db: Database =>
      db.transaction[R, E, A](zio, commitOnFailure)
    }
  }

  override def autoCommit[R, E, A](
      zio: => ZIO[Connection with R, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZIO[Database with R, Either[DbException, E], A] = {
    ZIO.serviceWithZIO { db: Database =>
      db.autoCommit[R, E, A](zio)
    }
  }

  /** Creates a Database Layer which requires an existing ConnectionSource. */
  def fromConnectionSource(implicit trace: Trace): ZLayer[ConnectionSource, Nothing, Database]

  /** Creates a Tranzactio Connection, given a JDBC connection and a Blocking. Useful for some utilities. */
  def connectionFromJdbc(connection: => JdbcConnection)(implicit trace: Trace): ZIO[Any, Nothing, Connection]

  /** Commodity method: creates a Database Layer which includes its own ConnectionSource based on a DataSource. Most
   * connection pool implementations should be able to provide you a DataSource.
   *
   * When no implicit ErrorStrategies is available, the default ErrorStrategies will be used.
   */
  final def fromDatasource(implicit trace: Trace): ZLayer[DataSource, Nothing, Database] =
    ConnectionSource.fromDatasource >>> fromConnectionSource

  /** As `fromDatasource`, but provides a default ErrorStrategiesRef. When a method is called with no available implicit
   * ErrorStrategiesRef, the ErrorStrategiesRef in argument will be used. */
  final def fromDatasource(errorStrategies: ErrorStrategiesRef)(implicit trace: Trace): ZLayer[DataSource, Nothing, Database] =
    ConnectionSource.fromDatasource(errorStrategies) >>> fromConnectionSource

  /** As `fromDatasource(ErrorStrategiesRef)`, but an `ErrorStrategies` is provided through a layer instead of as a parameter. */
  final def fromDatasourceAndErrorStrategies(implicit trace: Trace): ZLayer[DataSource with ErrorStrategies, Nothing, Database] =
    ConnectionSource.fromDatasourceAndErrorStrategies >>> fromConnectionSource

}
