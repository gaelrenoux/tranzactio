package io.github.gaelrenoux.tranzactio.integration

import doobie.implicits._
import doobie.util.fragment.Fragment
import io.github.gaelrenoux.tranzactio.doobie._
import io.github.gaelrenoux.tranzactio.{ConnectionSource, JdbcLayers}
import samples.Person
import samples.doobie.PersonQueries
import zio.stream.ZStream
import zio.test._
import zio.{Chunk, Scope, ZIO, ZLayer}


/** Integration tests for Doobie */
// scalastyle:off magic.number
object DoobieStreamIT extends ITSpec {

  /** Layer is recreated on each test, to have a different database every time. */
  def myLayer: ZLayer[Scope, Nothing, Database with PersonQueries] =
    PersonQueries.live ++ (JdbcLayers.datasourceU >>> ConnectionSource.fromDatasource >>> Database.fromConnectionSource)

  val buffy: Person = Person("Buffy", "Summers")
  val giles: Person = Person("Rupert", "Giles")

  def stream[R, E, A](zio: ZIO[R, E, A], num: Int = 1): ZStream[R, E, A] =
    if (num == 0) ZStream.never
    else if (num == 1) ZStream.fromZIO(zio)
    else ZStream.fromZIO(zio).forever.take(num)

  val connectionCountQuery: TranzactIO[Int] = tzio(Fragment.const(connectionCountSql).query[Int].unique)

  private def wrap[E, A](z: ZIO[Database with PersonQueries, E, A]): ZIO[Scope, E, A] =
    z.provideSome[Scope](myLayer)

  def spec: MySpec = suite("Doobie-Stream Integration Tests")(
    testDataCommittedOnTransactionSuccess,
    testConnectionClosedOnTransactionSuccess,
    testDataRollbackedOnTransactionFailure,
    testDataCommittedOnTransactionFailure,
    testConnectionClosedOnTransactionFailure,
    testMultipleTransactions,
    testDataCommittedOnAutoCommitSuccess,
    testConnectionClosedOnAutoCommitSuccess,
    testDataRollbackedOnAutoCommitFailure,
    testConnectionClosedOnAutoCommitFailure,
    testStreamDoesNotLoadAllValues
  )

  private val testDataCommittedOnTransactionSuccess: MySpec = test("data committed on transaction success") {
    wrap {
      for {
        _ <- Database.transaction(PersonQueries.setup)
        _ <- Database.transactionOrDieStream(stream(PersonQueries.insert(buffy), 3)).runDrain
        persons <- Database.transactionOrDieStream(PersonQueries.listStream).runCollect
      } yield assertTrue(persons == Chunk(buffy, buffy, buffy))
    }
  }

  private val testConnectionClosedOnTransactionSuccess: MySpec = test("connection closed on transaction success") {
    wrap {
      for {
        _ <- Database.transaction(PersonQueries.setup)
        _ <- Database.transactionOrDieStream(stream(PersonQueries.insert(buffy), 3)).runDrain
        connectionCount <- Database.transaction(connectionCountQuery)
      } yield assertTrue(connectionCount == 1) // only the current connection
    }
  }

  private val testDataRollbackedOnTransactionFailure: MySpec = test("data rollbacked on transaction failure if commitOnFailure=false") {
    wrap {
      for {
        _ <- Database.transaction(PersonQueries.setup)
        _ <- Database.transactionOrDieStream(stream(PersonQueries.insert(buffy), 2) ++ PersonQueries.failingStream ++ stream(PersonQueries.insert(buffy), 2)).runDrain.flip
        persons <- Database.transactionOrDieStream(PersonQueries.listStream).runCollect
      } yield assertTrue(persons.isEmpty)
    }
  }

  private val testDataCommittedOnTransactionFailure: MySpec = test("data committed on transaction failure if commitOnFailure=true") {
    wrap {
      for {
        _ <- Database.transaction(PersonQueries.setup)
        _ <- Database.transactionOrDieStream(stream(PersonQueries.insert(buffy), 2) ++ PersonQueries.failingStream ++ stream(PersonQueries.insert(buffy), 2), commitOnFailure = true).runDrain.flip
        persons <- Database.transactionOrDieStream(PersonQueries.listStream).runCollect
      } yield assertTrue(persons == Chunk(buffy, buffy))
    }
  }

  private val testConnectionClosedOnTransactionFailure: MySpec = test("connection closed on transaction failure") {
    wrap {
      for {
        _ <- Database.transaction(PersonQueries.setup)
        _ <- Database.transactionOrDieStream(stream(PersonQueries.insert(buffy), 2) ++ PersonQueries.failingStream ++ stream(PersonQueries.insert(buffy), 2)).runDrain.flip
        connectionCount <- Database.transaction(connectionCountQuery)
      } yield assertTrue(connectionCount == 1) // only the current connection
    }
  }

  private val testMultipleTransactions: MySpec = test("multiple transactions") {
    wrap {
      for {
        _ <- Database.transaction(PersonQueries.setup)
        _ <- {
          val stream1 = Database.transactionOrDieStream(stream(PersonQueries.insert(buffy), 2))
          val stream2 = Database.transactionOrDieStream(PersonQueries.failingStream)
          val stream3 = Database.transactionOrDieStream(stream(PersonQueries.insert(giles), 2))
          (stream1 ++ stream2 ++ stream3).runDrain.flip
        }
        persons <- Database.transactionOrDieStream(PersonQueries.listStream).runCollect
      } yield assertTrue(persons == Chunk(buffy, buffy))
    }
  }

  private val testDataCommittedOnAutoCommitSuccess: MySpec = test("data committed on autoCommit success") {
    wrap {
      for {
        _ <- Database.autoCommit(PersonQueries.setup)
        _ <- Database.autoCommitStream(stream(PersonQueries.insert(buffy), 3)).runDrain
        persons <- Database.autoCommitStream(PersonQueries.listStream).runCollect
      } yield assertTrue(persons == Chunk(buffy, buffy, buffy))
    }
  }

  private val testConnectionClosedOnAutoCommitSuccess: MySpec = test("connection closed on autoCommit success") {
    wrap {
      for {
        _ <- Database.autoCommit(PersonQueries.setup)
        _ <- Database.autoCommitStream(stream(PersonQueries.insert(buffy), 3)).runDrain
        connectionCount <- Database.autoCommit(connectionCountQuery)
      } yield assertTrue(connectionCount == 1) // only the current connection
    }
  }

  private val testDataRollbackedOnAutoCommitFailure: MySpec = test("data committed on autoCommit failure") {
    wrap {
      for {
        _ <- Database.autoCommit(PersonQueries.setup)
        _ <- Database.autoCommitStream(stream(PersonQueries.insert(buffy), 2) ++ PersonQueries.failingStream ++ stream(PersonQueries.insert(buffy), 2)).runDrain.flip
        persons <- Database.autoCommitStream(PersonQueries.listStream).runCollect
      } yield assertTrue(persons == Chunk(buffy, buffy))
    }
  }

  private val testConnectionClosedOnAutoCommitFailure: MySpec = test("connection closed on autoCommit failure") {
    wrap {
      for {
        _ <- Database.autoCommit(PersonQueries.setup)
        _ <- Database.autoCommitStream(stream(PersonQueries.insert(buffy), 2) ++ PersonQueries.failingStream ++ stream(PersonQueries.insert(buffy), 2)).runDrain.flip
        connectionCount <- Database.autoCommit(connectionCountQuery)
      } yield assertTrue(connectionCount == 1) // only the current connection
    }
  }

  private val testStreamDoesNotLoadAllValues: MySpec = test("stream does not load all values when reading") {
    wrap {
      for {
        _ <- Database.autoCommit(PersonQueries.setup)
        _ <- Database.autoCommit(PersonQueries.insert(buffy))
        _ <- Database.autoCommit(PersonQueries.insert(giles))
        failingOnSecondStream = tzioStream {
          sql"""SELECT given_name, family_name FROM person""".query[Person]
            .streamWithChunkSize(1) // make sure it's read one by one
            .map { p =>
              if (p.givenName == "Rupert") throw new IllegalStateException // fail on the second one, if it's ever read
              else p
            }
        }
        dbStream = Database.autoCommitStream(failingOnSecondStream).take(1) // only keep one
        result <- dbStream.runCollect
      } yield assertTrue(result == Chunk(buffy))
    }
  }

}
