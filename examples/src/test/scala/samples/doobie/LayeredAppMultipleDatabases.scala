package samples.doobie

import io.github.gaelrenoux.tranzactio.DbException
import io.github.gaelrenoux.tranzactio.doobie._
import samples.{Conf, ConnectionPool, Person}
import zio._

/** A sample app with two databases for different set of queries. */
object LayeredAppMultipleDatabases extends zio.ZIOAppDefault {

  /** Marker trait for the first DB */
  trait Db1

  /** Marker trait for the second DB */
  trait Db2

  private val database1: ZLayer[Any, Any, DatabaseT[Db1]] = {
    // Fresh calls are required so that the confs and datasource aren't conflated with the other layer's
    val conf = Conf.live("samble-doobie-app-1").fresh
    val dbRecoveryConf = conf >>> ZLayer.fromFunction((_: Conf).dbRecovery).fresh
    val datasource = conf >>> ConnectionPool.live.fresh
    (datasource ++ dbRecoveryConf) >>> DatabaseT[Db1].fromDatasourceAndErrorStrategies
  }

  private val database2: ZLayer[Any, Any, DatabaseT[Db2]] = {
    // Fresh calls are required so that the confs and datasource aren't conflated with the other layer's
    val conf = Conf.live("samble-doobie-app-2").fresh
    val dbRecoveryConf = conf >>> ZLayer.fromFunction((_: Conf).dbRecovery).fresh
    val datasource = conf >>> ConnectionPool.live.fresh
    (datasource ++ dbRecoveryConf) >>> DatabaseT[Db2].fromDatasourceAndErrorStrategies
  }

  private val personQueries = PersonQueries.live

  type AppEnv = DatabaseT[Db1] with DatabaseT[Db2] with PersonQueries
  private val appEnv = database1 ++ database2 ++ personQueries

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] =
    for {
      _ <- Console.printLine("Starting the app")
      team <- myApp().provideLayer(appEnv)
      _ <- Console.printLine(team.mkString(", "))
    } yield ExitCode(0)

  /** Main code for the application. Results in a big ZIO depending on the AppEnv. */
  def myApp(): ZIO[PersonQueries with DatabaseT[Db2] with DatabaseT[Db1], DbException, List[Person]] = {
    val queries1: ZIO[PersonQueries with Connection, DbException, List[Person]] = for {
      _ <- Console.printLine("Creating the table").orDie
      _ <- PersonQueries.setup
      _ <- Console.printLine("Inserting the trio").orDie
      _ <- PersonQueries.insert(Person("Buffy", "Summers"))
      _ <- PersonQueries.insert(Person("Willow", "Rosenberg"))
      _ <- PersonQueries.insert(Person("Alexander", "Harris"))
      _ <- Console.printLine("Reading the trio").orDie
      trio <- PersonQueries.list
    } yield trio

    val queries2: ZIO[PersonQueries with Connection, DbException, List[Person]] = for {
      _ <- Console.printLine("Creating the table").orDie
      _ <- PersonQueries.setup
      _ <- Console.printLine("Inserting the mentor").orDie
      _ <- PersonQueries.insert(Person("Rupert", "Giles"))
      _ <- Console.printLine("Reading the mentor").orDie
      mentor <- PersonQueries.list
    } yield mentor

    val zTrio: ZIO[PersonQueries with DatabaseT[Db1], DbException, List[Person]] = DatabaseT[Db1].transactionOrWiden(queries1)
    val zMentor: ZIO[PersonQueries with DatabaseT[Db2], DbException, List[Person]] = DatabaseT[Db2].transactionOrWiden(queries2)

    for {
      trio <- zTrio
      mentor <- zMentor
    } yield trio ++ mentor
  }

}
