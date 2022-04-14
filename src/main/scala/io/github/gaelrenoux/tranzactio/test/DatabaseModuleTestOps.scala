package io.github.gaelrenoux.tranzactio.test

import io.github.gaelrenoux.tranzactio._
import zio.{Tag, ZEnvironment, ZIO, ZLayer}

/** Testing utilities on the Database module. */
trait DatabaseModuleTestOps[Connection] extends DatabaseModuleBase[Connection, DatabaseOps.ServiceOps[Connection]] {

  private[tranzactio] implicit val connectionTag: Tag[Connection]

  /** A Connection which is incapable of running anything, to use when unit testing (and the queries are actually stubbed,
   * so they do not need a Database). Trying to run actual queries against it will fail. */
  val noConnection: ZIO[Any, Nothing, Connection] = connectionFromJdbc(NoopJdbcConnection)

  /** A Database which is incapable of running anything, to use when unit testing (and the queries are actually stubbed,
   * so they do not need a Database). Trying to run actual queries against it will fail. */
  lazy val none: ZLayer[Any, Nothing, Database] =
    ZLayer.succeed {
      /* TODO: Declare this as a static service, no need for dynamic declaration anymore */
      new Service {
        override def transactionR[R, E, A](zio: ZIO[Connection with R, E, A], commitOnFailure: Boolean)
          (implicit errorStrategies: ErrorStrategiesRef): ZIO[R, Either[DbException, E], A] =
          noConnection.flatMap { c =>
            ZIO.environmentWith[R](_ ++ ZEnvironment(c))
              .flatMap(zio.provideEnvironment(_))
              .mapError(Right(_))
          }

        override def autoCommitR[R, E, A](zio: ZIO[Connection with R, E, A])
          (implicit errorStrategies: ErrorStrategiesRef): ZIO[R, Either[DbException, E], A] =
          noConnection.flatMap { c =>
            ZIO.environmentWith[R](_ ++ ZEnvironment(c))
              .flatMap(zio.provideEnvironment(_))
              .mapError(Right(_))
          }
      }
    }
}
