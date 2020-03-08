package io.github.gaelrenoux.tranzactio

import zio.{Has, Tagged, ZIO}

abstract class DatabaseModuleBase[Connection, Dbs <: DatabaseOps.ServiceOps[Connection] : Tagged]
  extends DatabaseOps.ModuleOps[Connection, Dbs] {

  override def transactionR[R <: Has[_], E, A](zio: ZIO[R with Connection, E, A]): ZIO[Has[_ <: Dbs] with R, Either[DbException, E], A] = {
    ZIO.accessM { db: Has[_ <: Dbs] =>
      db.get.transactionR[R, E, A](zio).provideSome[R] { r =>
        val env = r ++ Has(()) // needed for the compiler
        env
      }
    }
  }

  override def autoCommitR[R <: Has[_], E, A](zio: ZIO[R with Connection, E, A]): ZIO[Has[_ <: Dbs] with R, Either[DbException, E], A] = {
    ZIO.accessM { db: Has[_ <: Dbs] =>
      db.get.autoCommitR[R, E, A](zio).provideSome[R] { r =>
        val env = r ++ Has(()) // needed for the compiler
        env
      }
    }
  }
}
