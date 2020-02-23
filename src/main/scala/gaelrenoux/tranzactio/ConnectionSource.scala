package gaelrenoux.tranzactio

import java.sql.{Connection, DriverManager}

import javax.sql.DataSource
import zio.blocking._
import zio.clock.Clock
import zio.{RIO, ZIO}

/** A module able to provide connections. They typically come from a connection pool. */
trait ConnectionSource {
  val connectionSource: ConnectionSource.Service[Any]
}

object ConnectionSource {

  trait Service[R] {
    /** Main function: how to obtain a connection. Everything else has a default implementation in Live, but this needs
     * to be provided by the concrete instance. */
    def getConnection: RIO[Blocking, Connection]

    def openConnection: ZIO[Any, DbException, Connection]

    def setAutoCommit(c: Connection, autoCommit: Boolean): ZIO[Any, DbException, Unit]

    def commitConnection(c: Connection): ZIO[Any, DbException, Unit]

    def rollbackConnection(c: Connection): ZIO[Any, DbException, Unit]

    def closeConnection(c: Connection): ZIO[Any, DbException, Unit]
  }

  /** ConnectionSource with standard behavior. Children class need to implement `getConnection`. */
  trait Live extends ConnectionSource with Blocking.Live with Clock.Live {
    self =>

    val errorStrategies: ErrorStrategies = ErrorStrategies.Default

    trait Service extends ConnectionSource.Service[Any] {
      // TODO handle error reporting when retrying

      def openConnection: ZIO[Any, DbException, Connection] = wrap(errorStrategies.openConnection) {
        getConnection.mapError(e => DbException.Wrapped(e))
      }

      def setAutoCommit(c: Connection, autoCommit: Boolean): ZIO[Any, DbException, Unit] = wrap(errorStrategies.setAutoCommit) {
        effectBlocking(c.setAutoCommit(autoCommit))
      }

      def commitConnection(c: Connection): ZIO[Any, DbException, Unit] = wrap(errorStrategies.commitConnection) {
        effectBlocking(c.commit())
      }

      def rollbackConnection(c: Connection): ZIO[Any, DbException, Unit] = wrap(errorStrategies.rollbackConnection) {
        effectBlocking(c.rollback())
      }

      /** Cannot fail */
      def closeConnection(c: Connection): ZIO[Any, DbException, Unit] = wrap(errorStrategies.closeConnection) {
        effectBlocking(c.close())
      }

      private def wrap[R, A](es: ErrorStrategy)(z: ZIO[Blocking, Throwable, A]) = es {
        z.mapError(e => DbException.Wrapped(e))
      }.provide(self)
    }

  }

  /** Implementation trait for a Live ConnectionSource based on a DriverManager: every time a connection is needed, the
   * DriverManager creates a new one (no pooling). This is useful for tests, but shouldn't be used in a live
   * application. Provides a new connection through the DriverManager every time one is needed. */
  trait FromDriverManager extends ConnectionSource.Live {
    val driver: Option[String] = None
    val url: String
    val user: String
    val password: String

    override val connectionSource: Service = new Service {
      override def getConnection: RIO[Blocking, Connection] = effectBlocking {
        driver.foreach(Class.forName)
        DriverManager.getConnection(url, user, password)
      }
    }
  }

  /** Implementation trait for a Live ConnectionSource based on a javax.sql.DataSource: required connection are obtained
   * from the datasource. Most connection pool libraries will provide one of those. */
  trait FromDatasource extends ConnectionSource.Live {
    val datasource: DataSource

    override val connectionSource: Service = new Service {
      override def getConnection: RIO[Blocking, Connection] = effectBlocking {
        datasource.getConnection()
      }
    }
  }

}
