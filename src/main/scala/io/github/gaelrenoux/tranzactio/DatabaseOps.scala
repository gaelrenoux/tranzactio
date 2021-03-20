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

  /** Method that should be implemented by subclasses, to provide the connection. Full (not partial) application. */
  private[tranzactio] def transactionRFull[R <: Has[_], E, A](
      zio: ZIO[R with Connection, E, A],
      commitOnFailure: Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef): ZIO[R with R0, Either[DbException, E], A]

  /** Provides that ZIO with a Connection. A transaction will be opened before any actions in the ZIO, and closed
   * after. It will commit only if the ZIO succeeds, and rollback otherwise. Failures in the initial ZIO will be
   * wrapped in a Right in the error case of the resulting ZIO, with connection errors resulting in a failure with the
   * exception wrapped in a Left. */
  final def transactionR[R <: Has[_], E, A](
      zio: ZIO[Connection with R, E, A],
      commitOnFailure: Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R with R0, Either[DbException, E], A] =
    transactionRFull[R, E, A](zio, commitOnFailure)

  @deprecated("Use transactionR without specifying an environment", since = "1.3.0")
  def transactionR[R <: Has[_]]: TransactionRPartiallyApplied[R, Connection, R0] =
    new TransactionRPartiallyApplied[R, Connection, R0](this)

  /** As `transactionR`, where the only needed environment is the connection. */
  final def transaction[E, A](
      zio: ZIO[Connection, E, A],
      commitOnFailure: Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R0, Either[DbException, E], A] =
    transactionRFull[Has[Unit], E, A](zio, commitOnFailure).provideSome(mixHasUnit)

  /** As `transactionR`, but exceptions are simply widened to a common failure type. The resulting failure type is a
   * superclass of both DbException and the error type of the inital ZIO. */
  final def transactionOrWidenR[R <: Has[_], E >: DbException, A](
      zio: ZIO[Connection with R, E, A],
      commitOnFailure: Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R with R0, E, A] =
    transactionRFull[R, E, A](zio, commitOnFailure).mapError(_.fold(identity, identity))

  @deprecated("Use transactionOrWidenR without specifying an environment", since = "1.3.0")
  final def transactionOrWidenR[R <: Has[_]]: TransactionOrWidenRPartiallyApplied[R, Connection, R0] =
    new TransactionOrWidenRPartiallyApplied[R, Connection, R0](this)

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
    transactionRFull[R, E, A](zio, commitOnFailure).flatMapError(dieOnLeft)

  @deprecated("Use transactionOrDieR without specifying an environment", since = "1.3.0")
  final def transactionOrDieR[R <: Has[_]]: TransactionOrDieRPartiallyApplied[R, Connection, R0] =
    new TransactionOrDieRPartiallyApplied[R, Connection, R0](this)

  /** As `transactionOrDieR`, where the only needed environment is the connection. */
  final def transactionOrDie[E, A](
      zio: ZIO[Connection, E, A],
      commitOnFailure: Boolean = false
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R0, E, A] =
    transaction[E, A](zio, commitOnFailure).flatMapError(dieOnLeft)


  /** Method that should be implemented by subclasses, to provide the connection. Full (not partial) application. */
  private[tranzactio] def autoCommitRFull[R <: Has[_], E, A](
      zio: ZIO[R with Connection, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef): ZIO[R with R0, Either[DbException, E], A]

  /** Provides that ZIO with a Connection. All DB action in the ZIO will be auto-committed. Failures in the initial
   * ZIO will be wrapped in a Right in the error case of the resulting ZIO, with connection errors resulting in a
   * failure with the exception wrapped in a Left. */
  final def autoCommitR[R <: Has[_], E, A](
      zio: ZIO[Connection with R, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R with R0, Either[DbException, E], A] =
    autoCommitRFull[R, E, A](zio)

  @deprecated("Use autoCommitR without specifying an environment", since = "1.3.0")
  def autoCommitR[R <: Has[_]]: AutoCommitRPartiallyApplied[R, Connection, R0] =
    new AutoCommitRPartiallyApplied[R, Connection, R0](this)

  /** As `autoCommitR`, where the only needed environment is the connection. */
  final def autoCommit[E, A](zio: ZIO[Connection, E, A])(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R0, Either[DbException, E], A] =
    autoCommitRFull[Has[Unit], E, A](zio).provideSome(mixHasUnit)

  /** As `autoCommitR`, but exceptions are simply widened to a common failure type. The resulting failure type is a
   * superclass of both DbException and the error type of the inital ZIO. */
  final def autoCommitOrWidenR[R <: Has[_], E >: DbException, A](
      zio: ZIO[Connection with R, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R with R0, E, A] =
    autoCommitRFull[R, E, A](zio).mapError(_.fold(identity, identity))

  @deprecated("Use autoCommitOrWidenR without specifying an environment", since = "1.3.0")
  final def autoCommitOrWidenR[R <: Has[_]]: AutoCommitOrWidenRPartiallyApplied[R, Connection, R0] =
    new AutoCommitOrWidenRPartiallyApplied[R, Connection, R0](this)

  /** As `autoCommitOrWidenR`, where the only needed environment is the connection. */
  final def autoCommitOrWiden[E >: DbException, A](zio: ZIO[Connection, E, A])(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R0, E, A] =
    autoCommit[E, A](zio).mapError(_.fold(identity, identity))

  /** As `autoCommitR`, but errors when handling the connections are treated as defects instead of failures. */
  final def autoCommitOrDieR[R <: Has[_], E, A](
      zio: ZIO[Connection with R, E, A]
  )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R with R0, E, A] =
    autoCommitRFull[R, E, A](zio).flatMapError(dieOnLeft)

  @deprecated("Use autoCommitOrWidenR without specifying an environment", since = "1.3.0")
  final def autoCommitOrDieR[R <: Has[_]]: AutoCommitOrDieRPartiallyApplied[R, Connection, R0] =
    new AutoCommitOrDieRPartiallyApplied[R, Connection, R0](this)

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

  private[tranzactio] final class TransactionRPartiallyApplied[R <: Has[_], Connection, R0](val parent: DatabaseOps[Connection, R0]) extends AnyVal {
    def apply[E, A](
        zio: ZIO[R with Connection, E, A],
        commitOnFailure: Boolean = false
    )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R with R0, Either[DbException, E], A] =
      parent.transactionRFull[R, E, A](zio, commitOnFailure)
  }

  private[tranzactio] final class TransactionOrWidenRPartiallyApplied[R <: Has[_], Connection, R0](val parent: DatabaseOps[Connection, R0]) extends AnyVal {
    def apply[E >: DbException, A](
        zio: ZIO[R with Connection, E, A],
        commitOnFailure: Boolean = false
    )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R with R0, E, A] =
      parent.transactionRFull[R, E, A](zio, commitOnFailure).mapError(_.fold(identity, identity))
  }

  private[tranzactio] final class TransactionOrDieRPartiallyApplied[R <: Has[_], Connection, R0](val parent: DatabaseOps[Connection, R0]) extends AnyVal {
    def apply[E, A](
        zio: ZIO[R with Connection, E, A],
        commitOnFailure: Boolean = false
    )(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R with R0, E, A] =
      parent.transactionRFull[R, E, A](zio, commitOnFailure).flatMapError(dieOnLeft)
  }

  private[tranzactio] final class AutoCommitRPartiallyApplied[R <: Has[_], Connection, R0](val parent: DatabaseOps[Connection, R0]) extends AnyVal {
    def apply[E, A](zio: ZIO[R with Connection, E, A])(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R with R0, Either[DbException, E], A] =
      parent.autoCommitRFull[R, E, A](zio)
  }

  private[tranzactio] final class AutoCommitOrWidenRPartiallyApplied[R <: Has[_], Connection, R0](val parent: DatabaseOps[Connection, R0]) extends AnyVal {
    def apply[E >: DbException, A](zio: ZIO[R with Connection, E, A])(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R with R0, E, A] =
      parent.autoCommitRFull[R, E, A](zio).mapError(_.fold(identity, identity))
  }

  private[tranzactio] final class AutoCommitOrDieRPartiallyApplied[R <: Has[_], Connection, R0](val parent: DatabaseOps[Connection, R0]) extends AnyVal {
    def apply[E, A](zio: ZIO[R with Connection, E, A])(implicit errorStrategies: ErrorStrategiesRef = ErrorStrategies.Parent): ZIO[R with R0, E, A] =
      parent.autoCommitRFull[R, E, A](zio).flatMapError(dieOnLeft)
  }


  private def dieOnLeft[E](e: Either[DbException, E]): UIO[E] = e match {
    case Right(e) => ZIO.succeed(e)
    case Left(e) => ZIO.die(e)
  }

}
