package io.github.gaelrenoux.tranzactio.integration

import io.github.gaelrenoux.tranzactio.doobie._
import io.github.gaelrenoux.tranzactio.{ConnectionSource, JdbcLayers}
import org.typelevel.doobie.implicits._
import org.typelevel.doobie.util.fragment.Fragment
import samples.Person
import samples.doobie.PersonQueries
import zio.test._
import zio.{Chunk, Scope, ZIO, ZLayer}


/** Integration tests for Doobie */
object DoobieIT extends ITSpec {

  /** Layer is recreated on each test, to have a different database every time. */
  def myLayer: ZLayer[Scope, Nothing, Database with PersonQueries] =
    PersonQueries.live ++ (JdbcLayers.datasourceU >>> ConnectionSource.fromDatasource >>> Database.fromConnectionSource)

  val buffy: Person = Person("Buffy", "Summers")
  val giles: Person = Person("Rupert", "Giles")

  val connectionCountQuery: TranzactIO[Int] = tzio(Fragment.const(connectionCountSql).query[Int].unique)

  private def wrap[E, A](z: ZIO[Database with PersonQueries, E, A]): ZIO[Scope, E, A] =
    z.provideSome[Scope](myLayer)

  def spec: MySpec = suite("Doobie Integration Tests")(
    testDataCommittedOnTransactionSuccess,
    testConnectionClosedOnTransactionSuccess,
    testDataRollbackedOnTransactionFailure,
    testDataCommittedOnTransactionFailure,
    testConnectionClosedOnTransactionFailure,
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
        _ <- Database.transaction(PersonQueries.insert(buffy))
        persons <- Database.transaction(PersonQueries.list)
      } yield assertTrue(persons == List(buffy))
    }
  }

  private val testConnectionClosedOnTransactionSuccess: MySpec = test("connection closed on transaction success") {
    wrap {
      for {
        _ <- Database.transaction(PersonQueries.setup)
        _ <- Database.transaction(PersonQueries.insert(buffy))
        connectionCount <- Database.transaction(connectionCountQuery)
      } yield assertTrue(connectionCount == 1) // only the current connection
    }
  }

  private val testDataRollbackedOnTransactionFailure: MySpec = test("data rollbacked on transaction failure if commitOnFailure=false") {
    wrap {
      for {
        _ <- Database.transaction(PersonQueries.setup)
        _ <- Database.transaction(PersonQueries.insert(buffy) <*> PersonQueries.failing).flip
        persons <- Database.transaction(PersonQueries.list)
      } yield assertTrue(persons.isEmpty)
    }
  }

  private val testDataCommittedOnTransactionFailure: MySpec = test("data committed on transaction failure if commitOnFailure=true") {
    wrap {
      for {
        _ <- Database.transaction(PersonQueries.setup)
        _ <- Database.transaction(PersonQueries.insert(buffy) <*> PersonQueries.failing, commitOnFailure = true).flip
        persons <- Database.transaction(PersonQueries.list)
      } yield assertTrue(persons == List(buffy))
    }
  }

  private val testConnectionClosedOnTransactionFailure: MySpec = test("connection closed on transaction failure") {
    wrap {
      for {
        _ <- Database.transaction(PersonQueries.setup)
        _ <- Database.transaction(PersonQueries.insert(buffy) <*> PersonQueries.failing).flip
        connectionCount <- Database.transaction(connectionCountQuery)
      } yield assertTrue(connectionCount == 1) // only the current connection
    }
  }

  private val testDataCommittedOnAutoCommitSuccess: MySpec = test("data committed on autoCommit success") {
    wrap {
      for {
        _ <- Database.autoCommit(PersonQueries.setup)
        _ <- Database.autoCommit(PersonQueries.insert(buffy))
        persons <- Database.autoCommit(PersonQueries.list)
      } yield assertTrue(persons == List(buffy))
    }
  }

  private val testConnectionClosedOnAutoCommitSuccess: MySpec = test("connection closed on autoCommit success") {
    wrap {
      for {
        _ <- Database.autoCommit(PersonQueries.setup)
        _ <- Database.autoCommit(PersonQueries.insert(buffy))
        connectionCount <- Database.autoCommit(connectionCountQuery)
      } yield assertTrue(connectionCount == 1) // only the current connection
    }
  }

  private val testDataRollbackedOnAutoCommitFailure: MySpec = test("data committed on autoCommit failure") {
    wrap {
      for {
        _ <- Database.autoCommit(PersonQueries.setup)
        _ <- Database.autoCommit(PersonQueries.insert(buffy) <*> PersonQueries.failing).flip
        persons <- Database.autoCommit(PersonQueries.list)
      } yield assertTrue(persons == List(buffy))
    }
  }

  private val testConnectionClosedOnAutoCommitFailure: MySpec = test("connection closed on autoCommit failure") {
    wrap {
      for {
        _ <- Database.autoCommit(PersonQueries.setup)
        _ <- Database.autoCommit(PersonQueries.insert(buffy))
        connectionCount <- Database.autoCommit(connectionCountQuery)
      } yield assertTrue(connectionCount == 1) // only the current connection
    }
  }

  private val testStreamDoesNotLoadAllValues: MySpec = test("stream does not load all values") {
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
        zio = failingOnSecondStream.take(1).runCollect // only keep one
        result <- Database.autoCommit(zio)
      } yield assertTrue(result == Chunk(buffy))
    }
  }

}
