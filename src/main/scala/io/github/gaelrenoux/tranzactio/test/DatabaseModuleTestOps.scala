package io.github.gaelrenoux.tranzactio.test

import io.github.gaelrenoux.tranzactio._
import zio.{Tag, ZIO, ZLayer}

/** Testing utilities on the Database module. */
trait DatabaseModuleTestOps[Connection] extends DatabaseModuleBase[Connection, DatabaseOps.ServiceOps[Connection]] {

  private[tranzactio] implicit val connectionTag: Tag[Connection]

  /** A Connection which is incapable of running anything, to use when unit testing (and the queries are actually stubbed,
   * so they do not need a Database). Trying to run actual queries against it will fail. */
  def noConnection(env: TranzactioEnv): ZIO[Any, Nothing, Connection] = connectionFromJdbc(env, NoopJdbcConnection)
  
  /** A Database which is incapable of running anything, to use when unit testing (and the queries are actually stubbed,
   * so they do not need a Database). Trying to run actual queries against it will fail. */
  lazy val none: ZLayer[TranzactioEnv, Nothing, Database] = ???
    // ZLayer.fromFunction { b: TranzactioEnv =>
    // new Service {
    //   override def transactionR[R, E, A](zio: ZIO[Connection with R, E, A], commitOnFailure: Boolean)
    //     (implicit errorStrategies: ErrorStrategiesRef): ZIO[R, Either[DbException, E], A] =
    //     noConnection(b).flatMap { c =>
    //       zio.provideSome[R] { r: R =>
    //         r ++[Connection] c // does not compile without the explicit type
    //       }.mapError(Right(_))
    //     }

    //   override def autoCommitR[R, E, A](zio: ZIO[Connection with R, E, A])
    //     (implicit errorStrategies: ErrorStrategiesRef): ZIO[R, Either[DbException, E], A] =
    //     noConnection(b).flatMap { c =>
    //       zio.provideSome[R] { r: R =>
    //         r ++[Connection] c // does not compile without the explicit type
    //       }.mapError(Right(_))
    //     }
    // }
  // }
  // TODO
}
