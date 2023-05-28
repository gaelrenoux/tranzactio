package samples.anorm.test

import io.github.gaelrenoux.tranzactio.anorm._
import samples.anorm.PersonQueries
import zio.ZLayer
import zio.test._


/** This is a test where you check you business methods, using stub queries. */
object SomeTest extends ZIOSpec[TestEnvironment with Database with PersonQueries] {
  type Env = TestEnvironment with Database with PersonQueries
  type MySpec = Spec[Env, Any]

  /** Using a 'none' Database, because we're not actually using it */
  override def bootstrap: ZLayer[Any, Any, Env] = testEnvironment ++ PersonQueries.test ++ Database.none

  override def spec: MySpec = suite("My tests with Anorm")(
    myTest
  )

  val myTest: MySpec = test("some test on a method")(
    for {
      h <- Database.transaction(PersonQueries.list)
      // do something with that result
    } yield assertTrue(h == Nil)
  )

}
