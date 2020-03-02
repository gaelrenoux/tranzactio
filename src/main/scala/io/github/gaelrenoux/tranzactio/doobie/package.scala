package io.github.gaelrenoux.tranzactio

import zio.ZIO

/** TranzactIO module for Doobie. */
package object doobie extends Module {
  override type Database = DoobieDatabase
  override type Connection = DoobieConnection
  override type Query[A] = _root_.doobie.ConnectionIO[A]

  val Database: DoobieDatabase.type = DoobieDatabase
  val Connection: DoobieConnection.type = DoobieConnection

  override def tzio[A](q: Query[A]): TranzactIO[A] = ZIO.accessM[DoobieConnection](_.apply(q)).mapError(DbException.Wrapped)

}
