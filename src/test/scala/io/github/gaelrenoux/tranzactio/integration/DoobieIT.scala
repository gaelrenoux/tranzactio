package io.github.gaelrenoux.tranzactio.integration

import doobie.implicits._
import doobie.util.fragment.Fragment
import io.github.gaelrenoux.tranzactio.ConnectionSource
import io.github.gaelrenoux.tranzactio.doobie._
import samples.Person
import samples.doobie.PersonQueries
import zio.blocking.Blocking
import zio.test.Assertion._
import zio.test._
import zio.{ULayer, ZLayer}


/** Integration tests for Doobie */
object DoobieIT extends ITSpec[Database, PersonQueries] {

  override val dbLayer: ZLayer[ConnectionSource with Blocking, Nothing, Database] = Database.fromConnectionSource

  override val personQueriesLive: ULayer[PersonQueries] = PersonQueries.live

  val buffy: Person = Person("Buffy", "Summers")
  val giles: Person = Person("Rupert", "Giles")

  val connectionCountQuery: TranzactIO[Int] = tzio(Fragment.const(connectionCountSql).query[Int].unique)

  def spec: Spec = suite("Doobie Integration Tests")(
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

  private val testDataCommittedOnTransactionSuccess = testM("data committed on transaction success") {
    for {
      _ <- Database.transactionR(PersonQueries.setup)
      _ <- Database.transactionR(PersonQueries.insert(buffy))
      persons <- Database.transactionR(PersonQueries.list)
    } yield assert(persons)(equalTo(List(buffy)))
  }

  private val testConnectionClosedOnTransactionSuccess = testM("connection closed on transaction success") {
    for {
      _ <- Database.transactionR(PersonQueries.setup)
      _ <- Database.transactionR(PersonQueries.insert(buffy))
      connectionCount <- Database.transaction(connectionCountQuery)
    } yield assert(connectionCount)(equalTo(1)) // only the current connection
  }

  private val testDataRollbackedOnTransactionFailure = testM("data rollbacked on transaction failure if commitOnFailure=false") {
    for {
      _ <- Database.transactionR(PersonQueries.setup)
      _ <- Database.transactionR(PersonQueries.insert(buffy) &&& PersonQueries.failing).flip
      persons <- Database.transactionR(PersonQueries.list)
    } yield assert(persons)(equalTo(Nil))
  }

  private val testDataCommittedOnTransactionFailure = testM("data committed on transaction failure if commitOnFailure=true") {
    for {
      _ <- Database.transactionR(PersonQueries.setup)
      _ <- Database.transactionR(PersonQueries.insert(buffy) &&& PersonQueries.failing, commitOnFailure = true).flip
      persons <- Database.transactionR(PersonQueries.list)
    } yield assert(persons)(equalTo(List(buffy)))
  }

  private val testConnectionClosedOnTransactionFailure = testM("connection closed on transaction failure") {
    for {
      _ <- Database.transactionR(PersonQueries.setup)
      _ <- Database.transactionR(PersonQueries.insert(buffy) &&& PersonQueries.failing).flip
      connectionCount <- Database.transaction(connectionCountQuery)
    } yield assert(connectionCount)(equalTo(1)) // only the current connection
  }

  private val testDataCommittedOnAutoCommitSuccess = testM("data committed on autoCommit success") {
    for {
      _ <- Database.autoCommitR(PersonQueries.setup)
      _ <- Database.autoCommitR(PersonQueries.insert(buffy))
      persons <- Database.autoCommitR(PersonQueries.list)
    } yield assert(persons)(equalTo(List(buffy)))
  }

  private val testConnectionClosedOnAutoCommitSuccess = testM("connection closed on autoCommit success") {
    for {
      _ <- Database.autoCommitR(PersonQueries.setup)
      _ <- Database.autoCommitR(PersonQueries.insert(buffy))
      connectionCount <- Database.autoCommit(connectionCountQuery)
    } yield assert(connectionCount)(equalTo(1)) // only the current connection
  }

  private val testDataRollbackedOnAutoCommitFailure = testM("data rollbacked on autoCommit failure") {
    for {
      _ <- Database.autoCommitR(PersonQueries.setup)
      _ <- Database.autoCommitR(PersonQueries.insert(buffy) &&& PersonQueries.failing).flip
      persons <- Database.autoCommitR(PersonQueries.list)
    } yield assert(persons)(equalTo(List(buffy)))
  }

  private val testConnectionClosedOnAutoCommitFailure = testM("connection closed on autoCommit failure") {
    for {
      _ <- Database.autoCommitR(PersonQueries.setup)
      _ <- Database.autoCommitR(PersonQueries.insert(buffy))
      connectionCount <- Database.autoCommit(connectionCountQuery)
    } yield assert(connectionCount)(equalTo(1)) // only the current connection
  }

  private val testStreamDoesNotLoadAllValues = testM("stream does not load all values") {
    for {
      _ <- Database.autoCommitR(PersonQueries.setup)
      _ <- Database.autoCommitR(PersonQueries.insert(buffy))
      _ <- Database.autoCommitR(PersonQueries.insert(giles))
      result <- Database.autoCommit {
        val doobieStream = sql"""SELECT given_name, family_name FROM person""".query[Person]
          .streamWithChunkSize(1) // make sure it's read one by one
          .map { p =>
            if (p.givenName == "Rupert") throw new IllegalStateException // fail on the second one, if it's ever read
            else p
          }
        tzioStream(doobieStream).take(1).runHead // only keep one
      }
    } yield assert(result)(isSome(equalTo(buffy)))
  }

}
