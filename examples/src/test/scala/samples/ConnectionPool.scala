package samples

import org.h2.jdbcx.JdbcDataSource


import javax.sql.DataSource
import zio.ZIO
import zio.ZLayer

/**
 * Typically, you would use a Connection Pool like HikariCP. Here, we're just gonna use the JDBC H2 datasource directly.
 * Don't do that in production !
 */
object ConnectionPool {

  val live: ZLayer[Conf, Throwable, DataSource] = ZLayer.fromZIO(
    ZIO.serviceWithZIO[Conf] { conf =>
      ZIO.attemptBlocking {
        val ds = new JdbcDataSource
        ds.setURL(conf.db.url)
        ds.setUser(conf.db.username)
        ds.setPassword(conf.db.password)
        ds
      }
    }
  )

}
