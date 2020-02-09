package gaelrenoux.tranzactio.doobie

import java.sql.{Connection => SqlConnection}

import cats.effect.Resource
import doobie.free.KleisliInterpreter
import doobie.util.transactor.{Strategy, Transactor}
import gaelrenoux.tranzactio.utils._
import gaelrenoux.tranzactio.{ConnectionSource, DatabaseApi, DatabaseWithConnectionSource, DbException}
import javax.sql.DataSource
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.interop.catz._
import zio.macros.delegate.Mix

trait DoobieDatabase extends DatabaseApi[Connection] {
  val database: DoobieDatabase.Service[Any]
}

object DoobieDatabase {

  type Service[R] = DatabaseApi.DatabaseServiceApi[R, Connection]

  trait Live extends DoobieDatabase with DatabaseWithConnectionSource[Connection] with Blocking.Live with Clock.Live {
    self =>

    override val database: Service[Any] = new ServiceWithConnectionSource {
      override def connectionFromSql(connection: SqlConnection): ZIO[Any, Nothing, Connection] = catsBlocker.map { b =>
        val connect = (c: SqlConnection) => Resource.pure[Task, SqlConnection](c)
        val interp = KleisliInterpreter[Task](b).ConnectionInterpreter
        val doobieTransactor = Transactor(connection, connect, interp, Strategy.void)
        new DoobieConnection.Live(doobieTransactor)
      }.provide(self)
    }
  }

  object > extends DoobieDatabase.Service[DoobieDatabase] {
    override def transactionR[R1, E, A](zio: ZIO[R1 with Connection, E, A])(
        implicit ev: R1 Mix Connection
    ): ZIO[R1 with DoobieDatabase, Either[DbException, E], A] =
      ZIO.accessM(_.database.transactionR[R1, E, A](zio))

    override def autoCommitR[R1, E, A](zio: ZIO[R1 with Connection, E, A])(
        implicit ev: R1 Mix Connection
    ): ZIO[R1 with DoobieDatabase, Either[DbException, E], A] =
      ZIO.accessM(_.database.autoCommitR[R1, E, A](zio))
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

