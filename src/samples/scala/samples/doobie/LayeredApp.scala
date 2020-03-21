package samples.doobie

import io.github.gaelrenoux.tranzactio.doobie._
import io.github.gaelrenoux.tranzactio.{DbException, ErrorStrategies}
import samples.Person
import zio.duration._
import zio.{ZEnv, ZIO, ZLayer, console}

/** A sample app where all modules are linked through ZLayer. Should run as is (make sure you have com.h2database:h2 in
 * your dependencies). */
object LayeredApp extends zio.App {

  type AppEnv = ZEnv with Database with PersonQueries

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {
    val prog = for {
      _ <- console.putStrLn("Loading configuration")
      conf <- loadDbConf()
      _ <- console.putStrLn("Setting up the env")
      appEnv = appLayer(conf)
      _ <- console.putStrLn("Calling the app")
      trio <- myApp().provideLayer(appEnv)
      _ <- console.putStrLn(trio.mkString(", "))
    } yield 0

    prog.orDie
  }

  def appLayer(dbConf: DbConf): ZLayer[ZEnv, Nothing, AppEnv] =
    ZEnv.any ++
      PersonQueries.live ++
      Database.fromDriverManager(
        dbConf.url, dbConf.username, dbConf.password,
        errorStrategies = ErrorStrategies.RetryForever.withTimeout(10.seconds).withRetryTimeout(1.minute)
      )

  def loadDbConf(): ZIO[Any, Nothing, DbConf] = ZIO.succeed(DbConf("jdbc:h2:mem:test", "sa", "sa"))

  case class DbConf(url: String, username: String, password: String)

  /** Main code for the application. Results in a big ZIO depending on the AppEnv. */
  def myApp(): ZIO[AppEnv, DbException, List[Person]] = {
    val queries: ZIO[Connection with AppEnv, DbException, List[Person]] = for {
      _ <- console.putStrLn("Creating the table")
      _ <- PersonQueries.setup
      _ <- console.putStrLn("Inserting the trio")
      _ <- PersonQueries.insert(Person("Buffy", "Summers"))
      _ <- PersonQueries.insert(Person("Willow", "Rosenberg"))
      _ <- PersonQueries.insert(Person("Alexander", "Harris"))
      _ <- console.putStrLn("Reading the trio")
      trio <- PersonQueries.list
    } yield trio

    Database.transactionOrWidenR[AppEnv](queries)
  }

}
