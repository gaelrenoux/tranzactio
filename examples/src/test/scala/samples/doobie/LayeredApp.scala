package samples.doobie

import doobie.util.log.LogHandler
import io.github.gaelrenoux.tranzactio.doobie._
import io.github.gaelrenoux.tranzactio.{DbException, ErrorStrategiesRef}
import samples.{Conf, ConnectionPool, Person}
import zio._
import zio.interop.catz._

/** A sample app where all modules are linked through ZLayer. Should run as is (make sure you have com.h2database:h2 in
 * your dependencies). */
object LayeredApp extends zio.ZIOAppDefault {

  private val conf = Conf.live("samble-doobie-app")
  private val dbRecoveryConf = conf >>> ZLayer.fromFunction((_: Conf).dbRecovery)
  private val datasource = conf >>> ConnectionPool.live
  // The DoobieDbContext is optional, default is to have the noop LogHandler
  implicit val doobieContext: DbContext = DbContext(logHandler = LogHandler.jdkLogHandler[Task])
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
    val queries: ZIO[Connection with AppEnv, DbException, List[Person]] = for {
      _ <- Console.printLine("Creating the table").orDie
      _ <- PersonQueries.setup
      _ <- Console.printLine("Inserting the trio").orDie
      _ <- PersonQueries.insert(Person("Buffy", "Summers"))
      _ <- PersonQueries.insert(Person("Willow", "Rosenberg"))
      _ <- PersonQueries.insert(Person("Alexander", "Harris"))
      _ <- Console.printLine("Reading the trio").orDie
      trio <- PersonQueries.list
    } yield trio

    ZIO.serviceWithZIO[Conf] { conf =>
      // if this implicit is not provided, tranzactio will use Conf.dbRecovery instead
      implicit val errorRecovery: ErrorStrategiesRef = conf.alternateDbRecovery
      Database.transactionOrWiden(queries)
    }
  }

}
