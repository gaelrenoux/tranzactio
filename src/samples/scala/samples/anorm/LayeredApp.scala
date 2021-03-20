package samples.anorm

import io.github.gaelrenoux.tranzactio.anorm._
import io.github.gaelrenoux.tranzactio.{DbException, ErrorStrategiesRef}
import samples.{Conf, ConnectionPool, Person}
import zio._

/** A sample app where all modules are linked through ZLayer. Should run as is (make sure you have com.h2database:h2 in
 * your dependencies). */
object LayeredApp extends zio.App {

  private val zenv = ZEnv.any
  private val conf = Conf.live("samble-anorm-app")
  private val dbRecoveryConf = conf >>> ZLayer.fromService { (c: Conf.Root) => c.dbRecovery }
  private val datasource = (conf ++ zenv) >>> ConnectionPool.live
  private val database = (datasource ++ zenv ++ dbRecoveryConf) >>> Database.fromDatasourceAndErrorStrategies
  private val personQueries = PersonQueries.live

  type AppEnv = ZEnv with Database with PersonQueries with Conf
  private val appEnv = zenv ++ conf ++ database ++ personQueries

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] = {
    val prog = for {
      _ <- console.putStrLn("Starting the app")
      trio <- myApp().provideLayer(appEnv)
      _ <- console.putStrLn(trio.mkString(", "))
    } yield ExitCode(0)

    prog.orDie
  }

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

    ZIO.accessM[AppEnv] { env =>
      // if this implicit is not provided, tranwactio will use Conf.Root.dbRecovery instead
      implicit val errorRecovery: ErrorStrategiesRef = env.get[Conf.Root].alternateDbRecovery
      Database.transactionOrWidenR(queries)
    }
  }

}
