package gaelrenoux.tranzactio.doobie

import java.sql.{Connection => SqlConnection}

import cats.effect.Resource
import doobie.free.KleisliInterpreter
import doobie.util.transactor.{Strategy, Transactor}
import gaelrenoux.tranzactio._
import gaelrenoux.tranzactio.utils._
import javax.sql.DataSource
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.interop.catz._
import zio.macros.delegate.Mix

/** A Database wrapping Doobie. Factory method are on the companion objects. */
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
  def fromDriverManager(
      url: String, user: String, password: String,
      driver: Option[String] = None, errorStrategies: ErrorStrategies = ErrorStrategies.Default
  ): Database.Live = {
    val (u, usr, pwd, d, es) = (url, user, password, driver, errorStrategies)
    new DoobieDatabase.Live with ConnectionSource.FromDriverManager {
      override val errorStrategies: ErrorStrategies = es
      override val driver: Option[String] = d
      override val url: String = u
      override val user: String = usr
      override val password: String = pwd
    }
  }

  /** Commodity method */
  def fromDatasource(datasource: DataSource, errorStrategies: ErrorStrategies = ErrorStrategies.Default
  ): Database.Live = {
    val (ds, es) = (datasource, errorStrategies)
    new DoobieDatabase.Live with ConnectionSource.FromDatasource {
      override val errorStrategies: ErrorStrategies = es
      override val datasource: DataSource = ds
    }
  }

}

