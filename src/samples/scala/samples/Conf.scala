package samples

import io.github.gaelrenoux.tranzactio.ErrorStrategies
import zio.duration._
import zio.{Has, Layer, ZLayer}

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
      dbRecovery = ErrorStrategies.timeout(10.seconds).retryForeverExponential(10.seconds, maxDelay = 10.seconds)
    )
  )


}

