package io.github.gaelrenoux.tranzactio

import zio.{Has, ZIO}

/** Common API for all Database services. */
trait DatabaseServiceApi[Connection] {

  /** Provides that ZIO with a Connection. A transaction will be opened before any actions in the ZIO, and closed
   * after. It will commit only if the ZIO succeeds, and rollback otherwise. Failures in the initial ZIO will be
   * wrapped in a Right in the error case of the resulting ZIO, with connection errors resulting in a failure with the
   * exception wrapped in a Left. */
  def transactionR[R <: Has[_], E, A](zio: ZIO[R with Connection, E, A]): ZIO[R, Either[DbException, E], A]

  /** As `transactionR`, where the only needed environment is the connection. */
  def transaction[E, A](zio: ZIO[Connection, E, A]): ZIO[Any, Either[DbException, E], A] =
    transactionR[Has[Unit], E, A](zio).provide(Has(()))

  /** As `transactionR`, but exceptions are simply widened to a common failure type. The resulting failure type is a
   * superclass of both DbException and the error type of the inital ZIO. */
  def transactionOrWidenR[R <: Has[_], E >: DbException, A](zio: ZIO[R with Connection, E, A]): ZIO[R, E, A] =
    transactionR[R, E, A](zio).mapError(_.fold(identity, identity))

  /** As `transactionOrWiden`, where the only needed environment is the connection. */
  def transactionOrWiden[E >: DbException, A](zio: ZIO[Connection, E, A]): ZIO[Any, E, A] =
    transactionOrWidenR[Has[Unit], E, A](zio).provide(Has(()))

  /** As `transactionR`, but errors when handling the connections are treated as defects instead of failures. */
  def transactionOrDieR[R <: Has[_], E, A](zio: ZIO[R with Connection, E, A]): ZIO[R, E, A] =
    transactionR[R, E, A](zio).flatMapError {
      case Right(e) => ZIO.succeed(e)
      case Left(e) => ZIO.die(e)
    }

  /** As `transactionOrDieR`, where the only needed environment is the connection. */
  def transactionOrDie[E >: DbException, A](zio: ZIO[Connection, E, A]): ZIO[Any, E, A] =
    transactionOrDieR[Has[Unit], E, A](zio).provide(Has(()))


  /** Provides that ZIO with a Connection. All DB action in the ZIO will be auto-committed. Failures in the initial
   * ZIO will be wrapped in a Right in the error case of the resulting ZIO, with connection errors resulting in a
   * failure with the exception wrapped in a Left. */
  def autoCommitR[R <: Has[_], E, A](zio: ZIO[R with Connection, E, A]): ZIO[R, Either[DbException, E], A]

  /** As `autoCommitR`, where the only needed environment is the connection. */
  def autoCommit[E, A](zio: ZIO[Connection, E, A]): ZIO[Any, Either[DbException, E], A] =
    autoCommitR[Has[Unit], E, A](zio).provide(Has(()))

  /** As `autoCommitR`, but exceptions are simply widened to a common failure type. The resulting failure type is a
   * superclass of both DbException and the error type of the inital ZIO. */
  def autoCommitOrWidenR[R <: Has[_], E >: DbException, A](zio: ZIO[R with Connection, E, A]): ZIO[R, E, A] =
    autoCommitR[R, E, A](zio).mapError(_.fold(identity, identity))

  /** As `autoCommitOrWidenR`, where the only needed environment is the connection. */
  def autoCommitOrWiden[E >: DbException, A](zio: ZIO[Connection, E, A]): ZIO[Any, E, A] =
    autoCommitOrWidenR[Has[Unit], E, A](zio).provide(Has(()))

  /** As `autoCommitR`, but errors when handling the connections are treated as defects instead of failures. */
  def autoCommitOrDieR[R <: Has[_], E, A](zio: ZIO[R with Connection, E, A]): ZIO[R, E, A] =
    autoCommitR[R, E, A](zio).flatMapError {
      case Right(e) => ZIO.succeed(e)
      case Left(e) => ZIO.die(e)
    }

  /** As `autoCommitOrDieR`, where the only needed environment is the connection. */
  def autoCommitOrDie[E >: DbException, A](zio: ZIO[Connection, E, A]): ZIO[Any, E, A] =
    autoCommitOrDieR[Has[Unit], E, A](zio).provide(Has(()))
}


