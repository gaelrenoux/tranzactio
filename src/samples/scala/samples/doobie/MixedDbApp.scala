package samples.doobie

import gaelrenoux.tranzactio.doobie._
import gaelrenoux.tranzactio.{DbException, ErrorStrategies}
import samples.Person
import zio.console.Console
import zio.macros.delegate.Mix
import zio.{RIO, Task, ZEnv, ZIO}

object MixedDbApp extends zio.App
  with PersonQueries.Live {

  type AppEnv = Database with ZEnv
  type AppTask[A] = ZIO[AppEnv, Throwable, A]

  val db: Database.Service[Any] =
    Database.fromDriverManager("jdbc:postgresql://localhost:54320/", "login", "password").database

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {
    val prog = for {
      conf <- loadConf()
      zenv <- RIO.environment[ZEnv]
      trio <- myApp().provide(appEnv(zenv, conf))
      _ <- ZIO.accessM[Console](_.console.putStrLn(trio.toString))
    } yield 0

    prog.orDie
  }

  def myApp(): ZIO[Database, DbException, List[Person]] = {
    val queries: TranzactIO[List[Person]] = for {
      _ <- personQueries.insert(Person("Buffy", "Summers"))
      _ <- personQueries.insert(Person("Willow", "Rosenberg"))
      _ <- personQueries.insert(Person("Alexander", "Harris"))
      trio <- personQueries.list
    } yield trio

    Database.>.transactionOrWiden(queries)
  }

  def loadConf(): Task[DbConf] = Task.succeed(DbConf("jdbc:whatever", "login", "password"))

  case class DbConf(url: String, username: String, password: String)

  private def appEnv(env: ZEnv, dbConf: DbConf)(implicit ev: Database Mix ZEnv): AppEnv = {
    val db = Database.fromDriverManager(dbConf.url, dbConf.username, dbConf.password, errorStrategies = ErrorStrategies.Default)
    ev.mix(db, env)
  }

}
