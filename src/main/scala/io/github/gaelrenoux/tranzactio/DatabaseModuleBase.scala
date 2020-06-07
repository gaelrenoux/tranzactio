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
  ): ZIO[Has[Dbs] with R, Either[DbException, E], A] = {
    ZIO.accessM { db: Has[Dbs] =>
      db.get[Dbs].transactionRFull[R, E, A](zio, commitOnFailure).provideSome[R] { r =>
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

  /** Commodity method: creates a Database Layer which includes its own ConnectionSource based on a DataSource. Most
   * connection pool implementations should be able to provide you a DataSource. */
  final def fromDatasource(
      errorStrategies: ErrorStrategies = ErrorStrategies.Brutal
  ): ZLayer[Has[DataSource] with Blocking with Clock, Nothing, Database] =
    (ConnectionSource.fromDatasource(errorStrategies) ++ Blocking.any) >>> fromConnectionSource

  val any: ZLayer[Database, Nothing, Database] = ZLayer.requires[Database]
}
