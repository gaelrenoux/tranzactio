package samples.doobie

import io.github.gaelrenoux.tranzactio.doobie._
import io.github.gaelrenoux.tranzactio.{DbException, ErrorStrategies}
import samples.Person
import zio.console.Console
import zio.{RIO, ZEnv, ZIO, ZLayer}

/** A sample app where all modules are linked through ZLayer. */
object LayeredApp extends zio.App {

  type AppEnv = ZEnv with Database with PersonQueries

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {
    val prog = for {
      conf <- loadDbConf()
      trio <- myApp().provideLayer(appLayer(conf))
      _ <- ZIO.accessM[Console](_.get[Console.Service].putStrLn(trio.toString))
    } yield 0

    prog.orDie
  }

  def appLayer(dbConf: DbConf): ZLayer[ZEnv, Nothing, AppEnv] =
    ZEnv.any ++
      PersonQueries.live ++
      Database.fromDriverManager(dbConf.url, dbConf.username, dbConf.password, errorStrategies = ErrorStrategies.Default)

  def loadDbConf(): ZIO[Any, Nothing, DbConf] = ZIO.succeed(DbConf("jdbc:whatever", "login", "password"))

  case class DbConf(url: String, username: String, password: String)

  def myApp(): ZIO[AppEnv, DbException, List[Person]] = {
    val queries: ZIO[Connection with AppEnv, DbException, List[Person]] = for {
      console <- RIO.environment[Console]
      _ <- console.get.putStrLn("Inserting the trio")
      _ <- PersonQueries.insert(Person("Buffy", "Summers"))
      _ <- PersonQueries.insert(Person("Willow", "Rosenberg"))
      _ <- PersonQueries.insert(Person("Alexander", "Harris"))
      _ <- console.get.putStrLn("Reading the trio")
      trio <- PersonQueries.list
    } yield trio

    Database.transactionOrWidenR[AppEnv, DbException, List[Person]](queries)
  }

}
