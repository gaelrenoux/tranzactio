package gaelrenoux.tranzactio

import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.{Schedule, ZIO}

/** How to handle issues in the various operations of the database. Note that this only applies to the operation
 * performed when handling the connection, and not the execution of the requests! */
case class ErrorStrategies(
    openConnection: ErrorStrategy,
    setAutoCommit: ErrorStrategy,
    commitConnection: ErrorStrategy,
    rollbackConnection: ErrorStrategy,
    closeConnection: ErrorStrategy
)

object ErrorStrategies {
  val Default: ErrorStrategies = all(ErrorStrategy.Default)

  /** No retries, and no timeout */
  val Nothing: ErrorStrategies = all(ErrorStrategy.Nothing)

  def all(s: ErrorStrategy): ErrorStrategies = ErrorStrategies(s, s, s, s, s)
}

case class ErrorStrategy(
    retrySchedule: Schedule[Clock, Any, Any],
    timeout: Duration,
    retryTimeout: Duration
) {

  def apply[R, A](z: ZIO[R, DbException, A]): ZIO[R with Clock with Blocking, DbException, A] =
    z
      .timeoutFail(DbException.Timeout(timeout))(timeout)
      .retry(retrySchedule)
      .timeoutFail(DbException.Timeout(retryTimeout))(retryTimeout)

  def noTimeout: ErrorStrategy = copy(timeout = Duration.Infinity, retryTimeout = Duration.Infinity)

  def noRetry: ErrorStrategy = copy(retrySchedule = Schedule.stop, retryTimeout = Duration.Infinity)

  def withTimeout(t: Duration): ErrorStrategy = copy(timeout = t)

}

object ErrorStrategy {
  val Default: ErrorStrategy =
    ErrorStrategy(Schedule.exponential(10.milliseconds) || Schedule.spaced(1.second), 10.seconds, 1.minute)

  val Nothing: ErrorStrategy =
    ErrorStrategy(Schedule.stop, Duration.Infinity, Duration.Infinity)
}

