package io.github.gaelrenoux.tranzactio.integration

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

  def spec: Spec = suite("Doobie Integration Tests")(
    testM("transaction commits on success") {
      for {
        _ <- Database.transactionR[PersonQueries](PersonQueries.setup)
        _ <- Database.transactionR[PersonQueries](PersonQueries.insert(buffy))
        persons <- Database.transactionR[PersonQueries](PersonQueries.list)
      } yield assert(persons)(equalTo(List(buffy)))
    },

    testM("transaction rollbacks on failure") {
      for {
        _ <- Database.transactionR[PersonQueries](PersonQueries.setup)
        _ <- Database.transactionR[PersonQueries](PersonQueries.insert(buffy) &&& PersonQueries.failing).flip
        persons <- Database.transactionR[PersonQueries](PersonQueries.list)
      } yield assert(persons)(equalTo(Nil))
    },

    testM("autoCommit commits on success") {
      for {
        _ <- Database.autoCommitR[PersonQueries](PersonQueries.setup)
        _ <- Database.autoCommitR[PersonQueries](PersonQueries.insert(buffy))
        persons <- Database.transactionR[PersonQueries](PersonQueries.list)
      } yield assert(persons)(equalTo(List(buffy)))
    },

    testM("autoCommit commits on failure") {
      for {
        _ <- Database.autoCommitR[PersonQueries](PersonQueries.setup)
        _ <- Database.autoCommitR[PersonQueries](PersonQueries.insert(buffy) &&& PersonQueries.failing).flip
        persons <- Database.transactionR[PersonQueries](PersonQueries.list)
      } yield assert(persons)(equalTo(List(buffy)))
    }

  )

}
