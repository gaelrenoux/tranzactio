package io.github.gaelrenoux.tranzactio.test

import io.github.gaelrenoux.tranzactio._
import zio.{Tag, ZEnvironment, ZIO, ZLayer, ZTraceElement}

/** Testing utilities on the Database module. */
trait DatabaseModuleTestOps[Connection] extends DatabaseModuleBase[Connection, DatabaseOps.ServiceOps[Connection]] {

  type AnyDatabase = DatabaseOps.ServiceOps[Connection]

  private[tranzactio] implicit val connectionTag: Tag[Connection]

  /** A Connection which is incapable of running anything, to use when unit testing (and the queries are actually stubbed,
   * so they do not need a Database). Trying to run actual queries against it will fail. */
  def noConnection(implicit trace: ZTraceElement): ZIO[Any, Nothing, Connection] = connectionFromJdbc(NoopJdbcConnection)

  /** A Database which is incapable of running anything, to use when unit testing (and the queries are actually stubbed,
   * so they do not need a Database). Trying to run actual queries against it will fail. */
  def none(implicit trace: ZTraceElement): ZLayer[Any, Nothing, AnyDatabase] =
    ZLayer.succeed {
      /* Can't extract this into a static class, both Service and Connection are local to the trait */
      new Service {
        override def transaction[R, E, A](zio: ZIO[Connection with R, E, A], commitOnFailure: Boolean)
          (implicit errorStrategies: ErrorStrategiesRef, trace: ZTraceElement): ZIO[R, Either[DbException, E], A] =
          noConnection.flatMap { c =>
            ZIO.environmentWith[R](_ ++ ZEnvironment(c))
              .flatMap(zio.provideEnvironment(_))
              .mapError(Right(_))
          }

        override def autoCommit[R, E, A](zio: ZIO[Connection with R, E, A])
          (implicit errorStrategies: ErrorStrategiesRef, trace: ZTraceElement): ZIO[R, Either[DbException, E], A] =
          noConnection.flatMap { c =>
            ZIO.environmentWith[R](_ ++ ZEnvironment(c))
              .flatMap(zio.provideEnvironment(_))
              .mapError(Right(_))
          }
      }
    }
}
