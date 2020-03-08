package io.github.gaelrenoux.tranzactio

import zio.{Has, ZIO}

/** TranzactIO module for Doobie. */
package object doobie extends Wrapper {
  override type Connection = Has[DoobieConnection.Service]
  override type DatabaseService = DoobieDatabase.Service
  override type Query[A] = _root_.doobie.ConnectionIO[A]

  val Database: DoobieDatabase.type = DoobieDatabase
  val Connection: DoobieConnection.type = DoobieConnection

  override def tzio[A](q: Query[A]): TranzactIO[A] = ZIO.accessM[Connection](_.get.apply(q)).mapError(DbException.Wrapped)

}
