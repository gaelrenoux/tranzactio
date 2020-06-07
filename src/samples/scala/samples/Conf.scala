package samples

import io.github.gaelrenoux.tranzactio.ErrorStrategies
import zio.{Has, Layer, ZLayer}
import zio.duration._

object Conf {

  case class Root(
      db: DbConf,
      dbRecovery: ErrorStrategies
  )

  case class DbConf(
      url: String,
      username: String,
      password: String
  )

  def live(dbName: String): Layer[Nothing, Has[Root]] = ZLayer.succeed(
    Conf.Root(
      db = DbConf(s"jdbc:h2:mem:$dbName;DB_CLOSE_DELAY=10", "sa", "sa"),
      dbRecovery = ErrorStrategies.RetryForever.withTimeout(10.seconds).withRetryTimeout(1.minute)
    )
  )


}

