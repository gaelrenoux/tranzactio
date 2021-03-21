package io.github.gaelrenoux.tranzactio.test

import io.github.gaelrenoux.tranzactio.{DatabaseModuleBase, DatabaseOps, DbException, ErrorStrategiesRef}
import izumi.reflect.Tag
import zio.blocking.Blocking
import zio.{Has, ZIO, ZLayer}

/** Testing utilities on the Database module. */
trait DatabaseModuleTestOps[Connection <: Has[_]] extends DatabaseModuleBase[Connection, DatabaseOps.ServiceOps[Connection]] {

  private[tranzactio] implicit val connectionTag: Tag[Connection]

  /** A Connection which is incapable of running anything, to use when unit testing (and the queries are actually stubbed,
   * so they do not need a Database). Trying to run actual queries against it will fail. */
  def noConnection(env: Blocking): ZIO[Any, Nothing, Connection] = connectionFromJdbc(env, NoopJdbcConnection)

  /** A Database which is incapable of running anything, to use when unit testing (and the queries are actually stubbed,
   * so they do not need a Database). Trying to run actual queries against it will fail. */
  lazy val none: ZLayer[Blocking, Nothing, Database] = ZLayer.fromFunction { b: Blocking =>
    new Service {
      override def transactionR[R <: Has[_], E, A](zio: ZIO[Connection with R, E, A], commitOnFailure: Boolean)
        (implicit errorStrategies: ErrorStrategiesRef): ZIO[R, Either[DbException, E], A] =
        noConnection(b).flatMap { c =>
          zio.provideSome[R] { r: R =>
            r ++[Connection] c // does not compile without the explicit type
          }.mapError(Right(_))
        }

      override def autoCommitR[R <: Has[_], E, A](zio: ZIO[Connection with R, E, A])
        (implicit errorStrategies: ErrorStrategiesRef): ZIO[R, Either[DbException, E], A] =
        noConnection(b).flatMap { c =>
          zio.provideSome[R] { r: R =>
            r ++[Connection] c // does not compile without the explicit type
          }.mapError(Right(_))
        }
    }
  }
}
