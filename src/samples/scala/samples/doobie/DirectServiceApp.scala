package samples.doobie

import java.io.IOException

import gaelrenoux.tranzactio.{DbException, ErrorStrategies, ErrorStrategy}
import gaelrenoux.tranzactio.doobie._
import samples.Person
import zio.ZIO

/** For a test run: load directly the service, and no retries so that it fails faster. */
object DirectServiceApp extends zio.App with PersonQueries.Live {

  val db: Database.Service[Any] =
    Database.fromDriverManager("jdbc:postgresql://localhost:54320/", "login", "password", errorStrategies = ErrorStrategies.Default.noRetry).database

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {

    val queries: TranzactIO[List[Person]] = for {
      _ <- personQueries.insert(Person("Buffy", "Summers"))
      _ <- personQueries.insert(Person("Willow", "Rosenberg"))
      _ <- personQueries.insert(Person("Alexander", "Harris"))
      trio <- personQueries.list
    } yield trio

    val queries2: ZIO[Connection, String, List[Person]] = queries.mapError(e => s"SQL error: $e")

    val prog: ZIO[Any, Either[DbException, String], Int] = for {
      result <- db.transaction(queries2)
    } yield result.size

    prog.orDieWith {
      case Left(th) => th
      case Right(str) => new Exception(str)
    }
  }

}
