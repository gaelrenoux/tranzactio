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
    def transaction[R1, E, A](zio: ZIO[R1 with Connection, E, A])(implicit ev: Mix[R1, Connection]): ZIO[R with R1, Either[DbException, E], A]

    /** As `transaction`, for an environment-less ZIO. */
    def transaction[E, A](zio: ZIO[Connection, E, A]): ZIO[R, Either[DbException, E], A] =
      transaction[Any, E, A](zio)(monomixRight[Connection])

    /** As `transaction`, but exceptions are simply widened to a common failure type. The resulting failure type is a
     * superclass of both DbException and the error type of the inital ZIO. */
    def transactionOrWiden[R1, E >: DbException, A](zio: ZIO[R1 with Connection, E, A])(implicit ev: Mix[R1, Connection]): ZIO[R with R1, E, A] =
      transaction[R1, E, A](zio).mapError(_.fold(identity, identity))

    /** As `transactionOrWiden`, for an environment-less ZIO. */
    def transactionOrWiden[E >: DbException, A](zio: ZIO[Connection, E, A]): ZIO[R, E, A] =
      transactionOrWiden[Any, E, A](zio)(monomixRight[Connection])

    /** As `transaction`, but errors when handling the connections are treated as defects instead of failures. */
    def transactionOrDie[R1, E, A](zio: ZIO[R1 with Connection, E, A])(implicit ev: Mix[R1, Connection]): ZIO[R with R1, E, A] =
      transaction[R1, E, A](zio).flatMapError {
        case Right(e) => ZIO.succeed(e)
        case Left(e) => ZIO.die(e)
      }

    /** As `transactionOrDie`, for an environment-less ZIO. */
    def transactionOrDie[E >: DbException, A](zio: ZIO[Connection, E, A]): ZIO[R, E, A] =
      transactionOrDie[Any, E, A](zio)(monomixRight[Connection])
  }

}

