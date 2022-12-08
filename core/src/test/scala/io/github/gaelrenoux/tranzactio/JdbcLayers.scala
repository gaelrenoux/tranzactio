package io.github.gaelrenoux.tranzactio

import org.h2.jdbcx.JdbcDataSource
import zio.test.testEnvironment
import zio.{ZIO, ZLayer}

import java.sql.{Connection, DriverManager}
import java.util.UUID
import javax.sql.DataSource

/** All layers are defs, not vals, so that every call (in different tests) will return a different layer, with its own DB. */
object JdbcLayers {

  /** Generates the DataSource layer.
   *
   * The H2 URL is based on an UUID, and the layer is executed for each test, so we have a different UUID on every test.
   *
   * Note that using H2, we need a delay to avoid dropping the DB when all connections are closed (between transactions
   * in a test). Another way to do this would be to handle a small connection pool (just one would be enough) but it
   * would make the test more complex.
   */
  def datasource: ZLayer[Any, Throwable, DataSource] = ZLayer.fromZIO(
    ZIO.attemptBlocking {
      val uuid = UUID.randomUUID().toString
      val ds = new JdbcDataSource
      ds.setURL(s"jdbc:h2:mem:$uuid;DB_CLOSE_DELAY=10")
      ds.setUser("sa")
      ds.setPassword("sa")
      ds
    }
  )

  def datasourceU: ZLayer[Any, Nothing, DataSource] = testEnvironment >>> datasource.orDie

  /** Generates a layer providing a single connection. Connection will not be closed until you close the JVM. */
  def connection: ZLayer[Any, Throwable, Connection] = ZLayer.fromZIO {
    ZIO.attemptBlocking {
      val uuid = UUID.randomUUID().toString
      DriverManager.getConnection(s"jdbc:h2:mem:$uuid;DB_CLOSE_DELAY=10", "sa", "sa")
    }
  }

  def connectionU: ZLayer[Any, Nothing, Connection] = testEnvironment >>> connection.orDie

}
