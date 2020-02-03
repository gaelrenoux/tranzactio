package gaelrenoux.tranzactio.doobie

import java.sql.{Connection => SqlConnection}

import cats.effect.Resource
import doobie.free.KleisliInterpreter
import doobie.util.transactor.{Strategy, Transactor}
import gaelrenoux.tranzactio.utils._
import gaelrenoux.tranzactio.{ConnectionSource, DbException, DbTemplate}
import javax.sql.DataSource
import zio._
import zio.interop.catz._
import zio.macros.delegate.Mix

trait DoobieDatabase extends DbTemplate[Connection] {
  val database: DoobieDatabase.Service[Any]
}

object DoobieDatabase {

  type Service[R] = DbTemplate.Service[R, Connection]

  trait Live extends DbTemplate.LiveBase[Connection] with DoobieDatabase with ConnectionSource {
    self =>

    override val database: Service[Any] = new LiveBaseService {
      override def connectionFromSql(connection: SqlConnection): ZIO[Any, Nothing, Connection] = catBlocker.map { b =>
        val connect = (c: SqlConnection) => Resource.pure[Task, SqlConnection](c)
        val interp = KleisliInterpreter[Task](b).ConnectionInterpreter
        val doobieTransactor = Transactor(connection, connect, interp, Strategy.void)
        new DoobieConnection.Live(doobieTransactor)
      }.provide(self)
    }
  }

  object > extends DoobieDatabase.Service[DoobieDatabase] {
    override def transaction[R1, E, A](zio: ZIO[R1 with Connection, E, A])(
        implicit ev: R1 Mix Connection
    ): ZIO[R1 with DoobieDatabase, Either[DbException, E], A] =
      ZIO.accessM(_.database.transaction[R1, E, A](zio))
  }


  /** Commodity method */
  def fromDriverManager(driver: String, url: String, user: String, password: String): Database.Live = {
    val params = (driver, url, user, password)
    new DoobieDatabase.Live with ConnectionSource.FromDriverManager {
      override val driver: String = params._1
      override val url: String = params._2
      override val user: String = params._3
      override val password: String = params._4
    }
  }

  /** Commodity method */
  def fromDatasource(ds: DataSource): Database.Live = {
    new DoobieDatabase.Live with ConnectionSource.FromDatasource {
      override val datasource: DataSource = ds
    }
  }

}

