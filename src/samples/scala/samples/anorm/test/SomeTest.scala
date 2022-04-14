package samples.anorm.test

import io.github.gaelrenoux.tranzactio.anorm._
import samples.anorm.PersonQueries
import zio.test.Assertion._
import zio.test._
import zio.{Scope, ZLayer}


/** This is a test where you check you business methods, using stub queries. */
object SomeTest extends ZIOSpec[TestEnvironment with Database with PersonQueries] {
  type Env = TestEnvironment with Database with PersonQueries
  type Spec = ZSpec[Env, Any]

  /** Using a 'none' Database, because we're not actually using it */
  override def layer: ZLayer[Scope, Any, Env] = testEnvironment ++ PersonQueries.test ++ Database.none

  override def spec: Spec = suite("My tests with Anorm")(
    myTest
  )

  val myTest: Spec = test("some test on a method")(
    for {
      h <- Database.transactionR(PersonQueries.list)
      // do something with that result
    } yield assert(h)(equalTo(Nil))
  )

}
