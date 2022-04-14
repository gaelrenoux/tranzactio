package io.github.gaelrenoux.tranzactio.integration

import doobie.implicits._
import doobie.util.fragment.Fragment
import io.github.gaelrenoux.tranzactio.doobie._
import io.github.gaelrenoux.tranzactio.{ConnectionSource, JdbcLayers}
import samples.Person
import samples.doobie.PersonQueries
import zio.test.Assertion._
import zio.test._
import zio.{Scope, ZIO, ZLayer}

import scala.annotation.nowarn


/** Integration tests for Doobie */
object DoobieIT extends ITSpec {

  /** Layer is recreated on each test, to have a different database every time. */
  def myLayer: ZLayer[Scope, Nothing, Database with PersonQueries] =
    PersonQueries.live ++ (JdbcLayers.datasourceU >>> ConnectionSource.fromDatasource >>> Database.fromConnectionSource)

  val buffy: Person = Person("Buffy", "Summers")
  val giles: Person = Person("Rupert", "Giles")

  val connectionCountQuery: TranzactIO[Int] = tzio(Fragment.const(connectionCountSql).query[Int].unique)

  private def wrap[E, A](z: ZIO[Database with PersonQueries, E, A]): ZIO[Scope, E, A] =
    z.provideSome(myLayer)

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

  private val testDataCommittedOnTransactionSuccess: Spec = test("data committed on transaction success") {
    wrap {
      for {
        _ <- Database.transactionR(PersonQueries.setup)
        _ <- Database.transactionR(PersonQueries.insert(buffy))
        persons <- Database.transactionR(PersonQueries.list)
      } yield assert(persons)(equalTo(List(buffy)))
    }
  }

  private val testConnectionClosedOnTransactionSuccess: Spec = test("connection closed on transaction success") {
    wrap {
      for {
        _ <- Database.transactionR(PersonQueries.setup)
        _ <- Database.transactionR(PersonQueries.insert(buffy))
        connectionCount <- Database.transaction(connectionCountQuery)
      } yield assert(connectionCount)(equalTo(1)) // only the current connection
    }
  }

  @nowarn("msg=a type was inferred to be `Any`; this may indicate a programming error.")
  private val testDataRollbackedOnTransactionFailure: Spec = test("data rollbacked on transaction failure if commitOnFailure=false") {
    wrap {
      for {
        _ <- Database.transactionR(PersonQueries.setup)
        _ <- Database.transactionR(PersonQueries.insert(buffy) <*> PersonQueries.failing).flip
        persons <- Database.transactionR(PersonQueries.list)
      } yield assert(persons)(equalTo(Nil))
    }
  }

  @nowarn("msg=a type was inferred to be `Any`; this may indicate a programming error.")
  private val testDataCommittedOnTransactionFailure: Spec = test("data committed on transaction failure if commitOnFailure=true") {
    wrap {
      for {
        _ <- Database.transactionR(PersonQueries.setup)
        _ <- Database.transactionR(PersonQueries.insert(buffy) <*> PersonQueries.failing, commitOnFailure = true).flip
        persons <- Database.transactionR(PersonQueries.list)
      } yield assert(persons)(equalTo(List(buffy)))
    }
  }

  private val testConnectionClosedOnTransactionFailure: Spec = test("connection closed on transaction failure") {
    wrap {
      for {
        _ <- Database.transactionR(PersonQueries.setup)
        _ <- Database.transactionR(PersonQueries.insert(buffy) <*> PersonQueries.failing).flip
        connectionCount <- Database.transaction(connectionCountQuery)
      } yield assert(connectionCount)(equalTo(1))
    } // only the current connection
  }

  private val testDataCommittedOnAutoCommitSuccess: Spec = test("data committed on autoCommit success") {
    wrap {
      for {
        _ <- Database.autoCommitR(PersonQueries.setup)
        _ <- Database.autoCommitR(PersonQueries.insert(buffy))
        persons <- Database.autoCommitR(PersonQueries.list)
      } yield assert(persons)(equalTo(List(buffy)))
    }
  }

  private val testConnectionClosedOnAutoCommitSuccess: Spec = test("connection closed on autoCommit success") {
    wrap {
      for {
        _ <- Database.autoCommitR(PersonQueries.setup)
        _ <- Database.autoCommitR(PersonQueries.insert(buffy))
        connectionCount <- Database.autoCommit(connectionCountQuery)
      } yield assert(connectionCount)(equalTo(1))
    } // only the current connection
  }

  @nowarn("msg=a type was inferred to be `Any`; this may indicate a programming error.")
  private val testDataRollbackedOnAutoCommitFailure: Spec = test("data rollbacked on autoCommit failure") {
    wrap {
      for {
        _ <- Database.autoCommitR(PersonQueries.setup)
        _ <- Database.autoCommitR(PersonQueries.insert(buffy) <*> PersonQueries.failing).flip
        persons <- Database.autoCommitR(PersonQueries.list)
      } yield assert(persons)(equalTo(List(buffy)))
    }
  }

  private val testConnectionClosedOnAutoCommitFailure: Spec = test("connection closed on autoCommit failure") {
    wrap {
      for {
        _ <- Database.autoCommitR(PersonQueries.setup)
        _ <- Database.autoCommitR(PersonQueries.insert(buffy))
        connectionCount <- Database.autoCommit(connectionCountQuery)
      } yield assert(connectionCount)(equalTo(1)) // only the current connection
    }
  }

  private val testStreamDoesNotLoadAllValues: Spec = test("stream does not load all values") {
    wrap {
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

}
