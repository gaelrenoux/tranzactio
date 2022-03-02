package samples.doobie

import io.github.gaelrenoux.tranzactio.doobie._
import io.github.gaelrenoux.tranzactio.{DbException, ErrorStrategiesRef}
import samples.{Conf, ConnectionPool, Person}
import zio._
import zio.Console

/** A sample app where all modules are linked through ZLayer. Should run as is (make sure you have com.h2database:h2 in
 * your dependencies). */
object LayeredApp extends zio.ZIOAppDefault {


  private val zenv = ZEnv.any
  private val conf = Conf.live("samble-doobie-app")
  private val dbRecoveryConf = conf >>> { (c: Conf.Root) => c.dbRecovery }.toLayer
  private val datasource = (conf ++ zenv) >>> ConnectionPool.live
  private val database = (datasource ++ zenv ++ dbRecoveryConf) >>> Database.fromDatasourceAndErrorStrategies
  private val personQueries = PersonQueries.live

  type AppEnv = ZEnv with Database with PersonQueries with Conf
  private val appEnv = zenv ++ conf ++ database ++ personQueries


  override def run: ZIO[Environment with ZEnv with ZIOAppArgs,Any,Any] = {
    val prog = for {
      _ <- Console.printLine("Starting the app")
      trio <- myApp().provideLayer(appEnv)
      _ <- Console.printLine(trio.mkString(", "))
    } yield ExitCode(0)

    prog
  }
  

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

    ZIO.environmentWithZIO[AppEnv] { env =>
      // if this implicit is not provided, tranzactio will use Conf.Root.dbRecovery instead
      implicit val errorRecovery: ErrorStrategiesRef = env.get[Conf.Root].alternateDbRecovery
      Database.transactionOrWidenR(queries)
    }
  }

}
