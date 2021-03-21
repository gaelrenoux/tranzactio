package io.github.gaelrenoux.tranzactio

import zio.{Has, UIO, ZIO}

/** Operations for a Database, based on a few atomic operations. Can be used both by the actual DB service, or by the DB
 * component where a Database is required in the resulting ZIO.
 * @tparam R0 Environment needed to run the operations.
 */
trait DatabaseOps[Connection, R0] {

  import DatabaseOps._

  /** How to mix a Has[Unit] in an R0. Needed to express `method` in term of `methodR`. */
  protected def mixHasUnit(r0: R0): R0 with Has[Unit]

  /** Provides that ZIO with a Connection. A transaction will be opened before any actions in the ZIO, and closed
   * after. It will commit only if the ZIO succeeds, and rollback otherwise. Failures in the initial ZIO will be
   * wrapped in a Right in the error case of the resulting ZIO, with connection errors resulting in a failure with the
   * exception wrapped in a Left.
   *
   * This method should be implemented by subclasses, to provide the connection.
   */
  def transactionR[R <: Has[_], E, A](
      zio: ZIO[Connection with R, E, A],
      commitOnFailure: Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R with R0, Either[DbException, E], A]

  /** As `transactionR`, where the only needed environment is the connection. */
  final def transaction[E, A](
      zio: ZIO[Connection, E, A],
      commitOnFailure: Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R0, Either[DbException, E], A] =
    transactionR[Has[Unit], E, A](zio, commitOnFailure).provideSome(mixHasUnit)

  /** As `transactionR`, but exceptions are simply widened to a common failure type. The resulting failure type is a
   * superclass of both DbException and the error type of the inital ZIO. */
  final def transactionOrWidenR[R <: Has[_], E >: DbException, A](
      zio: ZIO[Connection with R, E, A],
      commitOnFailure: Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R with R0, E, A] =
    transactionR[R, E, A](zio, commitOnFailure).mapError(_.fold(identity, identity))

  /** As `transactionOrWiden`, where the only needed environment is the connection. */
  final def transactionOrWiden[E >: DbException, A](
      zio: ZIO[Connection, E, A],
      commitOnFailure: Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R0, E, A] =
    transaction[E, A](zio, commitOnFailure).mapError(_.fold(identity, identity))

  /** As `transactionR`, but errors when handling the connections are treated as defects instead of failures. */
  final def transactionOrDieR[R <: Has[_], E, A](
      zio: ZIO[Connection with R, E, A],
      commitOnFailure: Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R with R0, E, A] =
    transactionR[R, E, A](zio, commitOnFailure).flatMapError(dieOnLeft)

  /** As `transactionOrDieR`, where the only needed environment is the connection. */
  final def transactionOrDie[E, A](
      zio: ZIO[Connection, E, A],
      commitOnFailure: Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R0, E, A] =
    transaction[E, A](zio, commitOnFailure).flatMapError(dieOnLeft)

  /** Provides that ZIO with a Connection. All DB action in the ZIO will be auto-committed. Failures in the initial
   * ZIO will be wrapped in a Right in the error case of the resulting ZIO, with connection errors resulting in a
   * failure with the exception wrapped in a Left.
   *
   * This method should be implemented by subclasses, to provide the connection.
   */
  def autoCommitR[R <: Has[_], E, A](
      zio: ZIO[Connection with R, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R with R0, Either[DbException, E], A]

  /** As `autoCommitR`, where the only needed environment is the connection. */
  final def autoCommit[E, A](zio: ZIO[Connection, E, A])(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R0, Either[DbException, E], A] =
    autoCommitR[Has[Unit], E, A](zio).provideSome(mixHasUnit)

  /** As `autoCommitR`, but exceptions are simply widened to a common failure type. The resulting failure type is a
   * superclass of both DbException and the error type of the inital ZIO. */
  final def autoCommitOrWidenR[R <: Has[_], E >: DbException, A](
      zio: ZIO[Connection with R, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R with R0, E, A] =
    autoCommitR[R, E, A](zio).mapError(_.fold(identity, identity))

  /** As `autoCommitOrWidenR`, where the only needed environment is the connection. */
  final def autoCommitOrWiden[E >: DbException, A](zio: ZIO[Connection, E, A])(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R0, E, A] =
    autoCommit[E, A](zio).mapError(_.fold(identity, identity))

  /** As `autoCommitR`, but errors when handling the connections are treated as defects instead of failures. */
  final def autoCommitOrDieR[R <: Has[_], E, A](
      zio: ZIO[Connection with R, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R with R0, E, A] =
    autoCommitR[R, E, A](zio).flatMapError(dieOnLeft)

  /** As `autoCommitOrDieR`, where the only needed environment is the connection. */
  final def autoCommitOrDie[E, A](zio: ZIO[Connection, E, A])(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R0, E, A] =
    autoCommit[E, A](zio).flatMapError(dieOnLeft)

}

object DatabaseOps {

  /** API for a Database service. Has[Unit] is used for the environment, as it has to be a Has, in place of Any. */
  trait ServiceOps[Connection] extends DatabaseOps[Connection, Any] {
    override protected final def mixHasUnit(r0: Any): Any with Has[Unit] = Has(())
  }

  /** API for commodity methods needing a Database. */
  trait ModuleOps[Connection, Dbs <: ServiceOps[Connection]] extends DatabaseOps[Connection, Has[Dbs]] {
    override protected final def mixHasUnit(r0: Has[Dbs]): Has[Dbs] with Has[Unit] = r0 ++ Has(())
  }

  private def dieOnLeft[E](e: Either[DbException, E]): UIO[E] = e match {
    case Right(e) => ZIO.succeed(e)
    case Left(e) => ZIO.die(e)
  }

}
