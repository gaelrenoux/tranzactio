package io.github.gaelrenoux.tranzactio

import zio.{Has, ZIO}

/** A specific wrapper package for one specific library (e.g. Doobie). */
trait Wrapper {

  /** The Connection that needs to be provided by the Database to run any Query. */
  type Connection

  /** The Database service provides a connection (transactionally or otherwise). */
  type DatabaseService <: DatabaseOps.ServiceOps[Connection]

  type Database = Has[DatabaseService]

  /** The specific type used in the wrapped library to represent an SQL query. */
  type Query[A]

  /** The type wrapping a Query[A] in TranzactIO. */
  type TranzactIO[A] = ZIO[Connection, DbException, A]

  /** Wraps a library-specific query into a TranzactIO. */
  def tzio[A](q: Query[A]): TranzactIO[A]

}
