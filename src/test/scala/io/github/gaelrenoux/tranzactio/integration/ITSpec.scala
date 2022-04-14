package io.github.gaelrenoux.tranzactio.integration

import io.github.gaelrenoux.tranzactio.ErrorStrategies
import zio._
import zio.test.{TestEnvironment, _}

abstract class ITSpec extends ZIOSpecDefault {
  type Spec = ZSpec[TestEnvironment with Scope, Any]

  implicit val errorStrategies: ErrorStrategies = ErrorStrategies.Nothing

  // TODO add aspect to timeout tests to 5 seconds

  val connectionCountSql = "select count(*) from information_schema.sessions"

}

