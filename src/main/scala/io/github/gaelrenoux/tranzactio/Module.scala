package io.github.gaelrenoux.tranzactio

import zio.ZIO

/** A Module is a wrapper for one specific library (e.g. Doobie). */
trait Module {

  /** The Database type provides a connection (transactionally or otherwise). It contains configuration on how to connect. */
  type Database

  /** The Connection that needs to be provided by the Database to run any Query. */
  type Connection

  /** The specific type used in the wrapped library to represent an SQL query. */
  type Query[A]

  /** The type wrapping a Query[A] in TranzactIO. */
  type TranzactIO[A] = ZIO[Connection, DbException, A]

  /** Wraps a library-specific query into a TranzactIO. */
  def tzio[A](q: Query[A]): TranzactIO[A]

}
