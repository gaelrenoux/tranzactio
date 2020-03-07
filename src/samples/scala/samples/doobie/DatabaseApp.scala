package samples.doobie

import io.github.gaelrenoux.tranzactio.doobie._
import io.github.gaelrenoux.tranzactio.{DbException, ErrorStrategies}
import samples.Person
import zio.console.Console
import zio.{RIO, ZEnv, ZIO, ZLayer}

object DatabaseApp extends zio.App
  with PersonQueries.Live {

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {
    val prog = for {
      conf <- loadDbConf()
      trio <- myApp().provideLayer(appLayer(conf))
      _ <- ZIO.accessM[Console](_.get[Console.Service].putStrLn(trio.toString))
    } yield 0

    prog.orDie
  }

  def appLayer(dbConf: DbConf): ZLayer[ZEnv, Nothing, Database with ZEnv] =
    ZEnv.any ++
      Database.fromDriverManager(dbConf.url, dbConf.username, dbConf.password, errorStrategies = ErrorStrategies.Default)

  def loadDbConf(): ZIO[Any, Nothing, DbConf] = ZIO.succeed(DbConf("jdbc:whatever", "login", "password"))

  case class DbConf(url: String, username: String, password: String)

  def myApp(): ZIO[Database with Console, DbException, List[Person]] = {
    val queries: ZIO[Connection with Console, DbException, List[Person]] = for {
      console <- RIO.environment[Console]
      _ <- console.get.putStrLn("Inserting the trio")
      _ <- personQueries.insert(Person("Buffy", "Summers"))
      _ <- personQueries.insert(Person("Willow", "Rosenberg"))
      _ <- personQueries.insert(Person("Alexander", "Harris"))
      _ <- console.get.putStrLn("Reading the trio")
      trio <- personQueries.list
    } yield trio

    Database.transactionOrWidenR[Console, DbException, List[Person]](queries)
  }

}
