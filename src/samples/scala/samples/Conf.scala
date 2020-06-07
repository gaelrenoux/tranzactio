package samples

import zio.{Has, Layer, ZLayer}

object Conf {

  case class Root(
      db: DbConf
  )

  case class DbConf(
      url: String,
      username: String,
      password: String
  )

  def live(dbName: String): Layer[Nothing, Has[Root]] = ZLayer.succeed(
    Conf.Root(
      db = DbConf(s"jdbc:h2:mem:$dbName;DB_CLOSE_DELAY=10", "sa", "sa"),
    )
  )


}

