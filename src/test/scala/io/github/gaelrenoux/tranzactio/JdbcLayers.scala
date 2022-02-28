package io.github.gaelrenoux.tranzactio

import org.h2.jdbcx.JdbcDataSource

import zio.test.testEnvironment
import zio.{ULayer, ZLayer}

import java.sql.{Connection, DriverManager}
import java.util.UUID
import javax.sql.DataSource
import zio.ZIO

object JdbcLayers {
  /** Generates the DataSource layer.
   *
   * The H2 URL is based on an UUID, and the layer is executed for each test, so we have a different UUID on every test.
   *
   * Note that using H2, we need a delay to avoid dropping the DB when all connections are closed (between transactions
   * in a test). Another way to do this would be to handle a small connection pool (just one would be enough) but it
   * would make the test more complex.
   */
  val datasource: ZLayer[Any, Throwable, DataSource] = ZIO.attemptBlocking {
    val ds = new JdbcDataSource
    ds.setURL(s"jdbc:h2:mem:${UUID.randomUUID().toString};DB_CLOSE_DELAY=10")
    ds.setUser("sa")
    ds.setPassword("sa")
    ds
  }.toLayer

  val datasourceU: ULayer[DataSource] = testEnvironment >>> datasource.orDie

  /** Generates a layer providing a single connection. Connection will not be closed until you close the JVM... */
  val connection: ZLayer[Any, Throwable, Connection] = ZIO.attemptBlocking {
    DriverManager.getConnection(s"jdbc:h2:mem:${UUID.randomUUID().toString};DB_CLOSE_DELAY=10", "sa", "sa")
  }.toLayer

  val connectionU: ULayer[Connection] = testEnvironment >>> connection.orDie

}
