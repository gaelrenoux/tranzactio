package gaelrenoux.tranzactio

import gaelrenoux.tranzactio.utils.monomixRight
import zio.ZIO
import zio.macros.delegate.Mix

/** Common template for all database services. */
trait DatabaseApi[Connection] {
  val database: DatabaseApi.DatabaseServiceApi[Any, Connection]
}

object DatabaseApi {

  /** Common API for all Database services. */
  trait DatabaseServiceApi[R, Connection] {

    /** Provides that ZIO with a Connection. A transaction will be opened before any actions in the ZIO, and closed
     * after. It will commit only if the ZIO succeeds, and rollback otherwise. Failures in the initial ZIO will be
     * wrapped in a Right in the error case of the resulting ZIO, with connection errors resulting in a failure with the
     * exception wrapped in a Left. */
    def transactionR[R1, E, A](zio: ZIO[R1 with Connection, E, A])(implicit ev: Mix[R1, Connection]): ZIO[R with R1, Either[DbException, E], A]

    /** As `transactionR`, where the only needed environment is the connection. */
    def transaction[E, A](zio: ZIO[Connection, E, A]): ZIO[R, Either[DbException, E], A] =
      transactionR[Any, E, A](zio)(monomixRight[Connection])

    /** As `transactionR`, but exceptions are simply widened to a common failure type. The resulting failure type is a
     * superclass of both DbException and the error type of the inital ZIO. */
    def transactionOrWidenR[R1, E >: DbException, A](zio: ZIO[R1 with Connection, E, A])(implicit ev: Mix[R1, Connection]): ZIO[R with R1, E, A] =
      transactionR[R1, E, A](zio).mapError(_.fold(identity, identity))

    /** As `transactionOrWiden`, where the only needed environment is the connection. */
    def transactionOrWiden[E >: DbException, A](zio: ZIO[Connection, E, A]): ZIO[R, E, A] =
      transactionOrWidenR[Any, E, A](zio)(monomixRight[Connection])

    /** As `transactionR`, but errors when handling the connections are treated as defects instead of failures. */
    def transactionOrDieR[R1, E, A](zio: ZIO[R1 with Connection, E, A])(implicit ev: Mix[R1, Connection]): ZIO[R with R1, E, A] =
      transactionR[R1, E, A](zio).flatMapError {
        case Right(e) => ZIO.succeed(e)
        case Left(e) => ZIO.die(e)
      }

    /** As `transactionOrDieR`, where the only needed environment is the connection. */
    def transactionOrDie[E >: DbException, A](zio: ZIO[Connection, E, A]): ZIO[R, E, A] =
      transactionOrDieR[Any, E, A](zio)(monomixRight[Connection])


    /** Provides that ZIO with a Connection. All DB action in the ZIO will be auto-committed. Failures in the initial
     * ZIO will be wrapped in a Right in the error case of the resulting ZIO, with connection errors resulting in a
     * failure with the exception wrapped in a Left. */
    def autoCommitR[R1, E, A](zio: ZIO[R1 with Connection, E, A])(implicit ev: Mix[R1, Connection]): ZIO[R with R1, Either[DbException, E], A]

    /** As `autoCommitR`, where the only needed environment is the connection. */
    def autoCommit[E, A](zio: ZIO[Connection, E, A]): ZIO[R, Either[DbException, E], A] =
      autoCommitR[Any, E, A](zio)(monomixRight[Connection])

    /** As `autoCommitR`, but exceptions are simply widened to a common failure type. The resulting failure type is a
     * superclass of both DbException and the error type of the inital ZIO. */
    def autoCommitOrWidenR[R1, E >: DbException, A](zio: ZIO[R1 with Connection, E, A])(implicit ev: Mix[R1, Connection]): ZIO[R with R1, E, A] =
      autoCommitR[R1, E, A](zio).mapError(_.fold(identity, identity))

    /** As `autoCommitOrWidenR`, where the only needed environment is the connection. */
    def autoCommitOrWiden[E >: DbException, A](zio: ZIO[Connection, E, A]): ZIO[R, E, A] =
      autoCommitOrWidenR[Any, E, A](zio)(monomixRight[Connection])

    /** As `autoCommitR`, but errors when handling the connections are treated as defects instead of failures. */
    def autoCommitOrDieR[R1, E, A](zio: ZIO[R1 with Connection, E, A])(implicit ev: Mix[R1, Connection]): ZIO[R with R1, E, A] =
      autoCommitR[R1, E, A](zio).flatMapError {
        case Right(e) => ZIO.succeed(e)
        case Left(e) => ZIO.die(e)
      }

    /** As `autoCommitOrDieR`, where the only needed environment is the connection. */
    def autoCommitOrDie[E >: DbException, A](zio: ZIO[Connection, E, A]): ZIO[R, E, A] =
      autoCommitOrDieR[Any, E, A](zio)(monomixRight[Connection])
  }

}

