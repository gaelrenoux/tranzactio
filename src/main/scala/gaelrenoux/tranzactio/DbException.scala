package gaelrenoux.tranzactio

import zio.duration.Duration

/** All exceptions that may happen when working with the DB. */
sealed trait DbException extends Exception

object DbException {

  case class Wrapped(cause: Throwable) extends Exception(cause) with DbException

  case class Timeout(duration: Duration) extends Exception(s"Timeout after $duration") with DbException

}
