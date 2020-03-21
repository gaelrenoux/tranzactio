package io.github.gaelrenoux.tranzactio

import zio.{Has, Tagged, ZIO}

/** Template implementing the commodity methods for a Db module. */
abstract class DatabaseModuleBase[Connection, Dbs <: DatabaseOps.ServiceOps[Connection] : Tagged]
  extends DatabaseOps.ModuleOps[Connection, Dbs] {

  private[tranzactio]
  override def transactionRFull[R <: Has[_], E, A](zio: ZIO[R with Connection, E, A]): ZIO[Has[_ <: Dbs] with R, Either[DbException, E], A] = {
    ZIO.accessM { db: Has[_ <: Dbs] =>
      db.get[Dbs].transactionRFull[R, E, A](zio).provideSome[R] { r =>
        val env = r ++ Has(()) // needed for the compiler
        env
      }
    }
  }

  private[tranzactio]
  override def autoCommitRFull[R <: Has[_], E, A](zio: ZIO[R with Connection, E, A]): ZIO[Has[_ <: Dbs] with R, Either[DbException, E], A] = {
    ZIO.accessM { db: Has[_ <: Dbs] =>
      db.get[Dbs].autoCommitRFull[R, E, A](zio).provideSome[R] { r =>
        val env = r ++ Has(()) // needed for the compiler
        env
      }
    }
  }
}
