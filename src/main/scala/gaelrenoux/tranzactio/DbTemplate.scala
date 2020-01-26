package gaelrenoux.tranzactio

import java.sql.{Connection => SqlConnection}

import gaelrenoux.tranzactio.utils.monomixRight
import zio._
import zio.blocking._
import zio.clock.Clock
import zio.macros.delegate.Mix

trait DbTemplate[Connection] {
  val database: DbTemplate.Service[Any, Connection]
}

object DbTemplate {

  trait Service[R, Connection] {

    def transaction[R1, E, A](zio: ZIO[R1 with Connection, E, A])(implicit ev: Mix[R1, Connection]): ZIO[R with R1, Either[DbException, E], A]

    def transaction[E, A](zio: ZIO[Connection, E, A]): ZIO[R, Either[DbException, E], A] =
      transaction[Any, E, A](zio)(monomixRight[Connection])

    def transactionOrWiden[R1, E >: DbException, A](zio: ZIO[R1 with Connection, E, A])(implicit ev: Mix[R1, Connection]): ZIO[R with R1, E, A] =
      transaction[R1, E, A](zio).mapError(_.fold(identity, identity))

    def transactionOrWiden[E >: DbException, A](zio: ZIO[Connection, E, A]): ZIO[R, E, A] =
      transactionOrWiden[Any, E, A](zio)(monomixRight[Connection])

    def transactionOrDie[R1, E, A](zio: ZIO[R1 with Connection, E, A])(implicit ev: Mix[R1, Connection]): ZIO[R with R1, E, A] =
      transaction[R1, E, A](zio).flatMapError {
        case Right(e) => ZIO.succeed(e)
        case Left(e) => ZIO.die(e)
      }

    def transactionOrDie[E >: DbException, A](zio: ZIO[Connection, E, A]): ZIO[R, E, A] =
      transactionOrDie[Any, E, A](zio)(monomixRight[Connection])
  }


  trait Base[Connection] extends DbTemplate[Connection] with Blocking with Clock with ConnectionSource {
    self =>

    import connectionSource._

    trait Service extends DbTemplate.Service[Any, Connection] {

      def connectionFromSql(connection: SqlConnection): ZIO[Any, Nothing, Connection]

      override def transaction[R1, E, A](zio: ZIO[R1 with Connection, E, A])(implicit ev: R1 Mix Connection): ZIO[R1, Either[DbException, E], A] =
        for {
          r1 <- ZIO.environment[R1]
          a <- openConnection.bracket(closeConnection) { c: SqlConnection =>
            setNoAutoCommit(c)
              .as(c)
              .flatMap(connectionFromSql)
              .map(ev.mix(r1, _))
              .flatMap(zio.mapError(Right(_)).provide(_))
              .tapBoth(
                _ => rollbackConnection(c),
                _ => commitConnection(c)
              )
          }
        } yield a
    }

  }


}

