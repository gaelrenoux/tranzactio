package io.github.gaelrenoux.tranzactio


import zio.stream.ZStream
import zio.{Tag, Trace, ZIO, ZLayer}

import javax.sql.DataSource

/** Template implementing the commodity methods for a Db module. */
abstract class DatabaseModuleBase[Connection, Database <: DatabaseOps.ServiceOps[Connection] : Tag, DbContext]
  extends DatabaseOps.ModuleOps[Connection, Database] {

  type Service = DatabaseOps.ServiceOps[Connection]

  override def transaction[R, E, A](
      zio: => ZIO[Connection with R, E, A],
      commitOnFailure: => Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZIO[Database with R, Either[DbException, E], A] = {
    ZIO.serviceWithZIO[Database] { db =>
      db.transaction[R, E, A](zio, commitOnFailure)
    }
  }

  override def transactionOrDieStream[R, E, A](
      stream: => ZStream[Connection with R, E, A],
      commitOnFailure: => Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZStream[Database with R, E, A] = {
    ZStream.serviceWithStream[Database] { db =>
      db.transactionOrDieStream[R, E, A](stream, commitOnFailure)
    }
  }

  override def autoCommit[R, E, A](
      zio: => ZIO[Connection with R, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZIO[Database with R, Either[DbException, E], A] = {
    ZIO.serviceWithZIO[Database] { db =>
      db.autoCommit[R, E, A](zio)
    }
  }

  override def autoCommitStream[R, E, A](
      stream: => ZStream[Connection with R, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZStream[Database with R, Either[DbException, E], A] = {
    ZStream.serviceWithStream[Database] { db =>
      db.autoCommitStream[R, E, A](stream)
    }
  }

  /** Creates a Database Layer which requires an existing ConnectionSource. */
  def fromConnectionSource(implicit dbContext: DbContext, trace: Trace): ZLayer[ConnectionSource, Nothing, Database]

  /** Commodity method: creates a Database Layer which includes its own ConnectionSource based on a DataSource. Most
   * connection pool implementations should be able to provide you a DataSource.
   *
   * When no implicit ErrorStrategies is available, the default ErrorStrategies will be used.
   */
  final def fromDatasource(implicit dbContext: DbContext, trace: Trace): ZLayer[DataSource, Nothing, Database] =
    ConnectionSource.fromDatasource >>> fromConnectionSource

  /** As `fromDatasource`, but provides a default ErrorStrategiesRef. When a method is called with no available implicit
   * ErrorStrategiesRef, the ErrorStrategiesRef in argument will be used. */
  final def fromDatasource(errorStrategies: ErrorStrategiesRef)(implicit dbContext: DbContext, trace: Trace): ZLayer[DataSource, Nothing, Database] =
    ConnectionSource.fromDatasource(errorStrategies) >>> fromConnectionSource

  /** As `fromDatasource(ErrorStrategiesRef)`, but an `ErrorStrategies` is provided through a layer instead of as a parameter. */
  final def fromDatasourceAndErrorStrategies(implicit dbContext: DbContext, trace: Trace): ZLayer[DataSource with ErrorStrategies, Nothing, Database] =
    ConnectionSource.fromDatasourceAndErrorStrategies >>> fromConnectionSource

}
