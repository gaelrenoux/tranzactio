package samples.doobie.test

import io.github.gaelrenoux.tranzactio.doobie._
import samples.doobie.PersonQueries
import zio._
import zio.test.Assertion._
import zio.test._
import zio.test.environment._

/** This is a test where you check you business methods, using stub queries.  */
object SomeTest extends RunnableSpec[TestEnvironment with Database with PersonQueries, Any] {
  type Env = TestEnvironment with Database with PersonQueries
  type Spec = ZSpec[Env, Any]

  /** Using a 'none' Database, because we're not actually using it */
  lazy val database: ULayer[Database] = testEnvironment >>> Database.none

  /** Using PersonQueries.test her */
  override def runner: TestRunner[Env, Any] =
    TestRunner(TestExecutor.default(testEnvironment ++ database ++ PersonQueries.test))

  override def aspects: List[TestAspect[Nothing, Env, Nothing, Any]] = Nil


  def spec: Spec = suite("My tests with Doobie")(
    myTest
  )

  private val myTest = testM("some test on a method") {
    for {
      h <- Database.transactionR(PersonQueries.list)
      // do something with that result
    } yield assert(h)(equalTo(Nil))
  }

}
