package samples.doobie

import io.github.gaelrenoux.tranzactio.doobie._
import io.github.gaelrenoux.tranzactio.{DbException, ErrorStrategiesRef}
import samples.{Conf, ConnectionPool, Person}
import zio._
import zio.stream._

/** Same as LayeredApp, but using Doobie's stream (converted into ZIO stream). */
// scalastyle:off magic.number
object LayeredAppStreaming extends zio.ZIOAppDefault {

  private val conf = Conf.live("samble-doobie-app-streaming")
  private val dbRecoveryConf = conf >>> ZLayer.fromFunction((_: Conf).dbRecovery)
  private val datasource = conf >>> ConnectionPool.live
  private val database = (datasource ++ dbRecoveryConf) >>> Database.fromDatasourceAndErrorStrategies
  private val personQueries = PersonQueries.live

  type AppEnv = Database with PersonQueries with Conf
  private val appEnv = database ++ personQueries ++ conf

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] =
    for {
      _ <- Console.printLine("Starting the app")
      trio <- myApp().provideLayer(appEnv)
      _ <- Console.printLine(trio.mkString(", "))
    } yield ExitCode(0)

  /** Main code for the application. Results in a big ZIO depending on the AppEnv. */
  def myApp(): ZIO[AppEnv, DbException, List[Person]] = {
    val initQueries: ZIO[Connection with AppEnv, DbException, Unit] = for {
      _ <- Console.printLine("Creating the table").orDie
      _ <- PersonQueries.setup
      _ <- Console.printLine("Inserting the trio").orDie
      _ <- PersonQueries.insert(Person("Buffy", "Summers"))
      _ <- PersonQueries.insert(Person("Willow", "Rosenberg"))
      _ <- PersonQueries.insert(Person("Alexander", "Harris"))
      _ <- PersonQueries.insert(Person("Rupert", "Giles")) // insert one more!
      _ <- Console.printLine("Reading the trio").orDie
    } yield ()

    val resultQueryStream: ZStream[Connection with AppEnv, DbException, Person] = PersonQueries.listStream.take(3) // take only the first 3

    ZIO.serviceWithZIO[Conf] { conf =>
      // if this implicit is not provided, tranzactio will use Conf.dbRecovery instead
      implicit val errorRecovery: ErrorStrategiesRef = conf.alternateDbRecovery
      val init: ZIO[Database with AppEnv, DbException, Unit] = Database.transactionOrWiden(initQueries)
      val resultsStream: ZStream[Database with AppEnv, DbException, Person] = Database.transactionOrDieStream(resultQueryStream)
      val results: ZIO[Database with AppEnv, DbException, List[Person]] = resultsStream.runCollect.map(_.toList)
      init *> results
    }


  }

}
