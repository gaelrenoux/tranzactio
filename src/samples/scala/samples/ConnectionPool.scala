package samples

import org.h2.jdbcx.JdbcDataSource
import zio.blocking.Blocking
import zio.{Has, ZIO, ZLayer, blocking}

import javax.sql.DataSource

/**
 * Typically, you would use a Connection Pool like HikariCP. Here, we're just gonna use the JDBC H2 datasource directly.
 * Don't do that in production !
 */
object ConnectionPool {

  val live: ZLayer[Conf with Blocking, Throwable, Has[DataSource]] =
    ZIO.accessM[Conf with Blocking] { env =>
      val conf = env.get[Conf.Root]
      blocking.effectBlocking {
        val ds = new JdbcDataSource
        ds.setURL(conf.db.url)
        ds.setUser(conf.db.username)
        ds.setPassword(conf.db.password)
        ds
      }
    }.toLayer

}
