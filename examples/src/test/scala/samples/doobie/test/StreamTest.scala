package samples.doobie.test

import zio.stream.{ZSink, ZStream}
import zio.test.Assertion._
import zio.test._
import zio.{ZIO, ZLayer}

import io.github.gaelrenoux.tranzactio.DbException
import io.github.gaelrenoux.tranzactio.doobie._

import samples.doobie.PersonQueries
import samples.{Conf, ConnectionPool, Person}
object StreamTest extends ZIOSpec[TestEnvironment with PersonQueries with Database] {
  type Env = TestEnvironment with PersonQueries with Database
  type MySpec = Spec[Env, Any]

  private val conf = Conf.live("samble-doobie-app-streaming")
  private val dbRecoveryConf = conf >>> ZLayer.fromFunction((_: Conf).dbRecovery)
  private val datasource = conf >>> ConnectionPool.live
  private val database = (datasource ++ dbRecoveryConf) >>> Database.fromDatasourceAndErrorStrategies
  private val personQueries = PersonQueries.live

  private val setupLayer = ZLayer.fromZIO(Database.transactionOrDie(PersonQueries.setup)).orDie
  override def bootstrap: ZLayer[Any, Any, Env] = testEnvironment  ++ personQueries ++ database

  private val insertRow = PersonQueries.insert(Person("Buffy", "Summers"))

  private val failingStream = ZStream.fail(DbException.Wrapped(new Exception()))
  override def spec: MySpec = suite("Tests ZStream with Doobie")(
    test("Test ZStream works")(
      for {
        database <- ZIO.service[Database]
        streamPersons: ZStream[PersonQueries, Either[DbException, DbException], Person] = database.transactionStream(PersonQueries.listStream)
        _ <- database.transactionOrDie(insertRow.replicateZIO(4))
        persons <- streamPersons.run(ZSink.foldLeft(List[Person]()) { (ps, p) => p :: ps }).mapError(_.merge)
        // do something with that result
      } yield assertTrue(persons.length == 4)
    ),
    test("Test transaction boundary - fail outside - commit progress")(
      for {
        database <- ZIO.service[Database]
        streamPersons: ZStream[PersonQueries, Either[DbException, DbException], Person] = database.transactionStream(PersonQueries.listStream)
        insertStream = database.transactionStream(ZStream.fromZIO(insertRow).forever).mapError(_.merge)
        stream = insertStream.take(2) ++ failingStream ++ insertStream
        _ <- stream.runDrain.exit

        persons <- streamPersons.run(ZSink.foldLeft(List[Person]()) { (ps, p) => p :: ps }).mapError(_.merge)
      } yield assertTrue(persons.length == 2)
    ),
      test("Test transaction boundary - fail inside - rollback")(
        for {
          database <- ZIO.service[Database]
          streamPersons: ZStream[PersonQueries, Either[DbException, DbException], Person] =
            database.transactionStream(PersonQueries.listStream)
          insertStream = database.transactionStream(
            ZStream.fromZIO(insertRow).forever.take(2) ++ failingStream ++ ZStream.fromZIO(insertRow)
          ).mapError(_.merge)
          _ <- insertStream.runDrain.exit

          persons <- streamPersons.run(ZSink.foldLeft(List[Person]()) { (ps, p) => p :: ps }).mapError(_.merge)
        } yield assertTrue(persons.isEmpty)
    )
  ).provideSomeLayer(setupLayer) @@ TestAspect.sequential
}
