package samples.slick

import slick.jdbc.H2Profile.api._
import zio.ZIO

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global


object Sample extends App {

  val db = Database.forConfig("")


  val queries: DBIO[Seq[(Int, String, String)]] = for {
    list <- PersonQueries.persons.result
    _ <- PersonQueries.persons += (1, "Buffy", "Summers")
  } yield list

  val z = ZIO.succeed(42)

  db.run()

  try {
    val f = db.run(queries.transactionally)
    val lines = Await.result(f, Duration.Inf)
    lines.foreach(println)
  } finally db.close

}
