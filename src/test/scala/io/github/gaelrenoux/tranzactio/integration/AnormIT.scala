package io.github.gaelrenoux.tranzactio.integration

import anorm._
import io.github.gaelrenoux.tranzactio.anorm._
import io.github.gaelrenoux.tranzactio.{ConnectionSource, JdbcLayers}
import samples.Person
import samples.anorm.PersonQueries
import zio.test.Assertion._
import zio.test._
import zio.{Scope, ZIO, ZLayer}

/** Integration tests for Doobie */
object AnormIT extends ITSpec {

  /** Layer is recreated on each test, to have a different database every time. */
  def myLayer: ZLayer[Scope, Nothing, Database with PersonQueries] =
    PersonQueries.live ++ (JdbcLayers.datasourceU >>> ConnectionSource.fromDatasource >>> Database.fromConnectionSource)

  val buffy: Person = Person("Buffy", "Summers")

  val connectionCountQuery: TranzactIO[Int] = tzio(implicit c => SQL(connectionCountSql).as(SqlParser.int(1).single))

  private def wrap[E, A](z: ZIO[Database with PersonQueries, E, A]): ZIO[Scope, E, A] =
    z.provideSome(myLayer)

  def spec: Spec = suite("Anorm Integration Tests")(
    testDataCommittedOnTransactionSuccess,
    testConnectionClosedOnTransactionSuccess,
    testDataRollbackedOnTransactionFailure,
    testDataCommittedOnTransactionFailure,
    testConnectionClosedOnTransactionFailure,
    testDataCommittedOnAutoCommitSuccess,
    testConnectionClosedOnAutoCommitSuccess,
    testDataRollbackedOnAutoCommitFailure,
    testConnectionClosedOnAutoCommitFailure
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

  private val testDataRollbackedOnTransactionFailure: Spec = test("data rollbacked on transaction failure if commitOnFailure=false") {
    wrap {
      for {
        _ <- Database.transactionR(PersonQueries.setup)
        _ <- Database.transactionR(PersonQueries.insert(buffy).zip(PersonQueries.failing)).flip
        persons <- Database.transactionR(PersonQueries.list)
      } yield assert(persons)(equalTo(Nil))
    }
  }

  private val testDataCommittedOnTransactionFailure: Spec = test("data committed on transaction failure if commitOnFailure=true") {
    wrap {
      for {
        _ <- Database.transactionR(PersonQueries.setup)
        _ <- Database.transactionR(PersonQueries.insert(buffy).zip(PersonQueries.failing), commitOnFailure = true).flip
        persons <- Database.transactionR(PersonQueries.list)
      } yield assert(persons)(equalTo(List(buffy)))
    }
  }

  private val testConnectionClosedOnTransactionFailure: Spec = test("connection closed on transaction failure") {
    wrap {
      for {
        _ <- Database.transactionR(PersonQueries.setup)
        _ <- Database.transactionR(PersonQueries.insert(buffy).zip(PersonQueries.failing)).flip
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

  private val testDataRollbackedOnAutoCommitFailure: Spec = test("data rollbacked on autoCommit failure") {
    wrap {
      for {
        _ <- Database.autoCommitR(PersonQueries.setup)
        _ <- Database.autoCommitR(PersonQueries.insert(buffy).zip(PersonQueries.failing)).flip
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

}
