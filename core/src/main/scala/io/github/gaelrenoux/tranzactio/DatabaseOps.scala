package io.github.gaelrenoux.tranzactio

import zio.{UIO, ZIO, Trace}

/** Operations for a Database, based on a few atomic operations. Can be used both by the actual DB service, or by the DB
 * component where a Database is required in the resulting ZIO.
 * @tparam R0 Environment needed to run the operations.
 */
trait DatabaseOps[Connection, R0] {

  import DatabaseOps._

  /** Provides that ZIO with a Connection. A transaction will be opened before any actions in the ZIO, and closed
   * after. It will commit only if the ZIO succeeds, and rollback otherwise. Failures in the initial ZIO will be
   * wrapped in a Right in the error case of the resulting ZIO, with connection errors resulting in a failure with the
   * exception wrapped in a Left.
   *
   * This method should be implemented by subclasses, to provide the connection.
   */
  def transaction[R <: Any, E, A](
      zio: => ZIO[Connection with R, E, A],
      commitOnFailure: => Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZIO[R with R0, Either[DbException, E], A]

  @deprecated("Use transaction instead.", since = "0.4.0")
  final def transactionR[R, E, A](
      zio: => ZIO[Connection with R, E, A],
      commitOnFailure: => Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZIO[R with R0, Either[DbException, E], A] =
    transaction[R, E, A](zio, commitOnFailure)

  /** As `transaction`, but exceptions are simply widened to a common failure type. The resulting failure type is a
   * superclass of both DbException and the error type of the inital ZIO. */
  final def transactionOrWiden[R, E >: DbException, A](
      zio: => ZIO[Connection with R, E, A],
      commitOnFailure: => Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZIO[R with R0, E, A] =
    transaction[R, E, A](zio, commitOnFailure).mapError(_.fold(identity, identity))

  @deprecated("Use transactionOrWiden instead.", since = "4.0.0")
  final def transactionOrWidenR[R, E >: DbException, A](
      zio: => ZIO[Connection with R, E, A],
      commitOnFailure: => Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZIO[R with R0, E, A] =
    transactionOrWiden[R, E, A](zio, commitOnFailure)

  /** As `transaction`, but errors when handling the connections are treated as defects instead of failures. */
  final def transactionOrDie[R, E, A](
      zio: => ZIO[Connection with R, E, A],
      commitOnFailure: => Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZIO[R with R0, E, A] =
    transaction[R, E, A](zio, commitOnFailure).flatMapError(dieOnLeft)

  @deprecated("Use transactionOrDie instead.", since = "4.0.0")
  final def transactionOrDieR[R, E, A](
      zio: => ZIO[Connection with R, E, A],
      commitOnFailure: => Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZIO[R with R0, E, A] =
    transactionOrDie[R, E, A](zio, commitOnFailure)

  /** Provides that ZIO with a Connection. All DB action in the ZIO will be auto-committed. Failures in the initial
   * ZIO will be wrapped in a Right in the error case of the resulting ZIO, with connection errors resulting in a
   * failure with the exception wrapped in a Left.
   *
   * This method should be implemented by subclasses, to provide the connection.
   */
  def autoCommit[R, E, A](
      zio: => ZIO[Connection with R, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZIO[R with R0, Either[DbException, E], A]

  @deprecated("Use autoCommit instead.", since = "4.0.0")
  final def autoCommitR[R, E, A](
      zio: => ZIO[Connection with R, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZIO[R with R0, Either[DbException, E], A] =
    autoCommit[R, E, A](zio)

  /** As `autoCommit`, but exceptions are simply widened to a common failure type. The resulting failure type is a
   * superclass of both DbException and the error type of the inital ZIO. */
  final def autoCommitOrWiden[R, E >: DbException, A](
      zio: => ZIO[Connection with R, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZIO[R with R0, E, A] =
    autoCommit[R, E, A](zio).mapError(_.fold(identity, identity))

  @deprecated("Use autoCommitOrWiden instead.", since = "4.0.0")
  final def autoCommitOrWidenR[R, E >: DbException, A](
      zio: => ZIO[Connection with R, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZIO[R with R0, E, A] =
    autoCommitOrWiden[R, E, A](zio)

  /** As `autoCommit`, but errors when handling the connections are treated as defects instead of failures. */
  final def autoCommitOrDie[R, E, A](
      zio: => ZIO[Connection with R, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZIO[R with R0, E, A] =
    autoCommit[R, E, A](zio).flatMapError(dieOnLeft)

  @deprecated("Use autoCommitOrDie instead.", since = "4.0.0")
  final def autoCommitOrDieR[R, E, A](
      zio: => ZIO[Connection with R, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent, trace: Trace): ZIO[R with R0, E, A] =
    autoCommitOrDie[R, E, A](zio)

}

object DatabaseOps {

  /** API for a Database service. */
  trait ServiceOps[Connection] extends DatabaseOps[Connection, Any]

  /** API for commodity methods needing a Database. */
  trait ModuleOps[Connection, Database <: ServiceOps[Connection]] extends DatabaseOps[Connection, Database]

  private def dieOnLeft[E](e: Either[DbException, E])(implicit trace: Trace): UIO[E] = e match {
    case Right(appError) => ZIO.succeed(appError)
    case Left(dbError) => ZIO.die(dbError)
  }

}
