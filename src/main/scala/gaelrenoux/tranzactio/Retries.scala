package gaelrenoux.tranzactio

import zio.Schedule
import zio.clock.Clock
import zio.duration._


/** Configuration on how to retry operations on the connections source. */
case class Retries(
    openConnection: Schedule[Clock, Any, Any],
    setNoAutoCommit: Schedule[Clock, Any, Any],
    commitConnection: Schedule[Clock, Any, Any],
    rollbackConnection: Schedule[Clock, Any, Any],
    closeConnection: Schedule[Clock, Any, Any]
)

object Retries {
  val Default: Retries = all(Schedule.exponential(100.milliseconds) && Schedule.elapsed.whileOutput(_ < 10.seconds))

  def all(s: Schedule[Clock, Any, Any]): Retries = Retries(s, s, s, s, s)
}
