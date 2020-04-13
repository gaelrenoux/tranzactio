package io.github.gaelrenoux.tranzactio.integration

import doobie.util.fragment.Fragment
import io.github.gaelrenoux.tranzactio.ConnectionSource
import io.github.gaelrenoux.tranzactio.doobie._
import samples.Person
import samples.doobie.PersonQueries
import zio.ZLayer
import zio.blocking.Blocking
import zio.test.Assertion._
import zio.test._

/** Integration tests for Doobie */
object DoobieIT extends ITSpec[Database] {

  override val dbLayer: ZLayer[ConnectionSource with Blocking, Nothing, Database] = Database.fromConnectionSource

  val buffy: Person = Person("Buffy", "Summers")

  val connectionCountQuery: TranzactIO[Int] = tzio(Fragment.const(connectionCountSql).query[Int].unique)

  def spec: Spec = suite("Doobie Integration Tests")(
    testM("data committed on transaction success") {
      for {
        _ <- Database.transactionR[PersonQueries](PersonQueries.setup)
        _ <- Database.transactionR[PersonQueries](PersonQueries.insert(buffy))
        persons <- Database.transactionR[PersonQueries](PersonQueries.list)
      } yield assert(persons)(equalTo(List(buffy)))
    },

    testM("connection closed on transaction success") {
      for {
        _ <- Database.transactionR[PersonQueries](PersonQueries.setup)
        _ <- Database.transactionR[PersonQueries](PersonQueries.insert(buffy))
        connectionCount <- Database.transaction(connectionCountQuery)
      } yield assert(connectionCount)(equalTo(1)) // only the current connection
    },

    testM("data rollbacked on transaction failure") {
      for {
        _ <- Database.transactionR[PersonQueries](PersonQueries.setup)
        _ <- Database.transactionR[PersonQueries](PersonQueries.insert(buffy) &&& PersonQueries.failing).flip
        persons <- Database.transactionR[PersonQueries](PersonQueries.list)
      } yield assert(persons)(equalTo(Nil))
    },

    testM("connection closed on transaction failure") {
      for {
        _ <- Database.transactionR[PersonQueries](PersonQueries.setup)
        _ <- Database.transactionR[PersonQueries](PersonQueries.insert(buffy) &&& PersonQueries.failing).flip
        connectionCount <- Database.transaction(connectionCountQuery)
      } yield assert(connectionCount)(equalTo(1)) // only the current connection
    },

    testM("data committed on autoCommit success") {
      for {
        _ <- Database.autoCommitR[PersonQueries](PersonQueries.setup)
        _ <- Database.autoCommitR[PersonQueries](PersonQueries.insert(buffy))
        persons <- Database.autoCommitR[PersonQueries](PersonQueries.list)
      } yield assert(persons)(equalTo(List(buffy)))
    },

    testM("connection closed on autoCommit success") {
      for {
        _ <- Database.autoCommitR[PersonQueries](PersonQueries.setup)
        _ <- Database.autoCommitR[PersonQueries](PersonQueries.insert(buffy))
        connectionCount <- Database.autoCommit(connectionCountQuery)
      } yield assert(connectionCount)(equalTo(1)) // only the current connection
    },

    testM("data rollbacked on autoCommit failure") {
      for {
        _ <- Database.autoCommitR[PersonQueries](PersonQueries.setup)
        _ <- Database.autoCommitR[PersonQueries](PersonQueries.insert(buffy) &&& PersonQueries.failing).flip
        persons <- Database.autoCommitR[PersonQueries](PersonQueries.list)
      } yield assert(persons)(equalTo(List(buffy)))
    },

    testM("connection closed on autoCommit failure") {
      for {
        _ <- Database.autoCommitR[PersonQueries](PersonQueries.setup)
        _ <- Database.autoCommitR[PersonQueries](PersonQueries.insert(buffy))
        connectionCount <- Database.autoCommit(connectionCountQuery)
      } yield assert(connectionCount)(equalTo(1)) // only the current connection
    }

  )

}
