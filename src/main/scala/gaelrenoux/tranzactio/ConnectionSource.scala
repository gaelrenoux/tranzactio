package gaelrenoux.tranzactio

import java.sql.{Connection, DriverManager}

import javax.sql.DataSource
import zio.blocking._
import zio.clock.Clock
import zio.{RIO, URIO, ZIO}

/** A module able to provide connections. They typically come from a connection pool. */
trait ConnectionSource {
  val connectionSource: ConnectionSource.Service[Any]
}

object ConnectionSource {

  trait Service[R] {
    def getConnection: RIO[Blocking, Connection]

    val retries: Retries = Retries.Default

    def openConnection: ZIO[Any, Left[DbException, Nothing], Connection]

    def setNoAutoCommit(c: Connection): ZIO[Any, Left[DbException, Nothing], Unit]

    def commitConnection(c: Connection): ZIO[Any, Left[DbException, Nothing], Unit]

    def rollbackConnection(c: Connection): ZIO[Any, Left[DbException, Nothing], Unit]

    def closeConnection(c: Connection): URIO[Any, Unit]
  }

  trait Live extends ConnectionSource with Blocking.Live with Clock.Live {
    self =>

    val retries: Retries = Retries.Default

    trait Service extends ConnectionSource.Service[Any] {

      def openConnection: ZIO[Any, Left[DbException, Nothing], Connection] = wrap {
        getConnection.retry(retries.openConnection)
      }

      def setNoAutoCommit(c: Connection): ZIO[Any, Left[DbException, Nothing], Unit] = wrap {
        ZIO.effect(c.setAutoCommit(false)).retry(retries.setNoAutoCommit)
      }

      def commitConnection(c: Connection): ZIO[Any, Left[DbException, Nothing], Unit] = wrap {
        effectBlocking(c.commit()).retry(retries.commitConnection)
      }

      def rollbackConnection(c: Connection): ZIO[Any, Left[DbException, Nothing], Unit] = wrap {
        effectBlocking(c.rollback()).retry(retries.rollbackConnection)
      }

      def closeConnection(c: Connection): URIO[Any, Unit] =
        effectBlocking(c.close())
          .retry(retries.closeConnection)
          .orDie
          .provide(self)

      private def wrap[A](z: ZIO[Blocking with Clock, Throwable, A]): ZIO[Any, Left[DbException, Nothing], A] =
        z.mapError(e => Left(DbException(e))).provide(self)
    }

  }

  /** Useful for tests, but shouldn't be used in a live application. Provides a new connection through the DriverManager every time one is needed. */
  trait FromDriverManager extends ConnectionSource.Live {
    val driver: String
    val url: String
    val user: String
    val password: String

    override val connectionSource: Service = new Service {
      override def getConnection: RIO[Blocking, Connection] = effectBlocking {
        Class.forName(driver)
        DriverManager.getConnection(url, user, password)
      }
    }
  }

  /** Loads a connection source from a javax.sql.DataSource. Most connection pools should be able to provide one of those. */
  trait FromDatasource extends ConnectionSource.Live {
    val datasource: DataSource

    override val connectionSource: Service = new Service {
      override def getConnection: RIO[Blocking, Connection] = effectBlocking {
        datasource.getConnection()
      }
    }
  }

}
