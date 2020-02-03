package gaelrenoux.tranzactio.slick

import java.sql.{Connection => SqlConnection}

import cats.effect.Resource
import doobie.free.KleisliInterpreter
import doobie.util.transactor.{Strategy, Transactor}
import gaelrenoux.tranzactio.doobie.DoobieConnection
import gaelrenoux.tranzactio.utils._
import gaelrenoux.tranzactio.{ConnectionSource, DbException, DbTemplate}
import javax.sql.DataSource
import zio._
import zio.interop.catz._
import zio.macros.delegate.Mix

trait SlickDatabase extends DbTemplate[Connection] {
  val database: SlickDatabase.Service[Any]
}

object SlickDatabase {


  type Service[R] = DbTemplate.Service[R, Connection]

  trait Live extends DbTemplate.LiveBase[Connection] with SlickDatabase {
    self =>

    val slickDb: SDatabase

    override val database: Service[Any] = new Service[Any] {
      override def connectionFromSql(connection: SqlConnection): ZIO[Any, Nothing, Connection] = {




      }
    }
  }

  object > extends SlickDatabase.Service[SlickDatabase] {
    override def transaction[R1, E, A](zio: ZIO[R1 with Connection, E, A])(
        implicit ev: R1 Mix Connection
    ): ZIO[R1 with SlickDatabase, Either[DbException, E], A] =
      ZIO.accessM(_.database.transaction[R1, E, A](zio))
  }


}

