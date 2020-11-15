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
  override def transactionRFull[R <: Has[_], E, A](
      zio: ZIO[R with Connection, E, A],
      commitOnFailure: Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[Has[Dbs] with R, Either[DbException, E], A] = {
    ZIO.accessM { db: Has[Dbs] =>
      db.get[Dbs].transactionRFull[R, E, A](zio, commitOnFailure).provideSome[R] { r =>
        val env = r ++ Has(()) // needed for the compiler
        env
      }
    }
  }

  private[tranzactio]
  override def autoCommitRFull[R <: Has[_], E, A](
      zio: ZIO[R with Connection, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[Has[Dbs] with R, Either[DbException, E], A] = {
    ZIO.accessM { db: Has[Dbs] =>
      db.get[Dbs].autoCommitRFull[R, E, A](zio).provideSome[R] { r =>
        val env = r ++ Has(()) // needed for the compiler
        env
      }
    }
  }

  /** Creates a Database Layer which requires an existing ConnectionSource. */
  def fromConnectionSource: ZLayer[ConnectionSource with Blocking, Nothing, Database]

  /** Commodity method: creates a Database Layer which includes its own ConnectionSource based on a DataSource. Most
   * connection pool implementations should be able to provide you a DataSource.
   *
   * When no implicit ErrorStrategies is available, the default ErrorStrategies will be used.
   */
  final val fromDatasource: ZLayer[Has[DataSource] with Blocking with Clock, Nothing, Database] =
    (ConnectionSource.fromDatasource ++ Blocking.any) >>> fromConnectionSource

  /** As `fromDatasource`, but provides a default ErrorStrategiesRef. When a method is called with no available implicit
   * ErrorStrategiesRef, the ErrorStrategiesRef in argument will be used. */
  final def fromDatasource(errorStrategies: ErrorStrategiesRef): ZLayer[Has[DataSource] with Blocking with Clock, Nothing, Database] =
    (ConnectionSource.fromDatasource(errorStrategies) ++ Blocking.any) >>> fromConnectionSource

  /** As `fromDatasource(ErrorStrategiesRef)`, but an `ErrorStrategies` is provided through a layer instead of as a parameter. */
  final val fromDatasourceAndErrorStrategies: ZLayer[Has[DataSource] with Has[ErrorStrategies] with Blocking with Clock, Nothing, Database] =
    (ConnectionSource.fromDatasourceAndErrorStrategies ++ Blocking.any) >>> fromConnectionSource


  val any: ZLayer[Database, Nothing, Database] = ZLayer.requires[Database]
}
