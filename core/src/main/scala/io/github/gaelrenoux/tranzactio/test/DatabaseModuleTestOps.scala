package io.github.gaelrenoux.tranzactio.test

import zio.stream.ZStream

import io.github.gaelrenoux.tranzactio._
import zio.{Tag, Trace, ZEnvironment, ZIO, ZLayer}

/** Testing utilities on the Database module. */
trait DatabaseModuleTestOps[Connection, DbContext] extends DatabaseModuleBase[Connection, DatabaseOps.ServiceOps[Connection], DbContext] {

  type AnyDatabase = DatabaseOps.ServiceOps[Connection]

  private[tranzactio] implicit val connectionTag: Tag[Connection]

  /** A Connection which is incapable of running anything, to use when unit testing (and the queries are actually stubbed,
   * so they do not need a Database). Trying to run actual queries against it will fail. */
  def noConnection(implicit trace: Trace): ZIO[Any, Nothing, Connection]

  /** A Database which is incapable of running anything, to use when unit testing (and the queries are actually stubbed,
   * so they do not need a Database). Trying to run actual queries against it will fail. */
  def none(implicit trace: Trace): ZLayer[Any, Nothing, AnyDatabase] =
    ZLayer.succeed {
      /* Can't extract this into a static class, both Service and Connection are local to the trait */
      new Service {
        /**
         * @param commitOnFailure Unused.
         * @param errorStrategies Unused.
         */
        override def transaction[R, E, A](zio: => ZIO[Connection with R, E, A], commitOnFailure: => Boolean)
          (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R, Either[DbException, E], A] =
          noConnection.flatMap { c =>
            ZIO.environmentWith[R](_ ++ ZEnvironment(c))
              .flatMap(zio.provideEnvironment(_))
              .mapError(Right(_))
          }

        override def transactionStream[R, E, A](stream: => ZStream[Connection with R, E, A], commitOnFailure: => Boolean = false)
          (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZStream[R, Either[DbException, E], A] = {
          ZStream.fromZIO(noConnection).flatMap { c =>
            ZStream.environmentWith[R](_ ++ ZEnvironment(c))
              .flatMap(stream.provideEnvironment(_)).mapError(Right(_))
          }
        }

        override def autoCommit[R, E, A](zio: => ZIO[Connection with R, E, A])
          (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R, Either[DbException, E], A] =
          noConnection.flatMap { c =>
            ZIO.environmentWith[R](_ ++ ZEnvironment(c))
              .flatMap(zio.provideEnvironment(_))
              .mapError(Right(_))
          }

        override def autoCommitStream[R, E, A](stream: => ZStream[Connection with R, E, A])
          (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZStream[R, Either[DbException, E], A] =
          ZStream.fromZIO(noConnection).flatMap { c =>
            ZStream.environmentWith[R](_ ++ ZEnvironment(c))
              .flatMap(stream.provideEnvironment(_))
              .mapError(Right(_))
          }
      }
    }
}
