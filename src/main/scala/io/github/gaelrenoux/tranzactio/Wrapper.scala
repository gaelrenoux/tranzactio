package io.github.gaelrenoux.tranzactio

import zio.ZTraceElement


/** A specific wrapper package for one specific library (e.g. Doobie). */
trait Wrapper {

  /** The Connection that needs to be provided by the Database to run any Query. */
  type Connection

  /** The Database provides a connection (transactionally or otherwise). */
  type Database <: DatabaseOps.ServiceOps[Connection]

  val Database: DatabaseOps.ModuleOps[Connection, _ <: DatabaseOps.ServiceOps[Connection]] // scalastyle:ignore field.name

  /** The specific type used in the wrapped library to represent an SQL query. */
  type Query[A]

  /** The type wrapping a Query[A] in TranzactIO.
   *
   * Could be defined here (instead of separately on each module), but this confuses some IDEs (like IntelliJ). The
   * error appears when one file imports e.g. doobie.TranzactIO, and the other doobie._: the two TranzactIOs are not
   * considered to be the same type. By defining TranzactIO on each module, the error disappears.
   */
  type TranzactIO[A]

  /** Wraps a library-specific query into a TranzactIO. */
  def tzio[A](q: => Query[A])(implicit trace: ZTraceElement): TranzactIO[A]

}
