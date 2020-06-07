package io.github.gaelrenoux.tranzactio

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
) {

  def all(f: ErrorStrategy => ErrorStrategy): ErrorStrategies = ErrorStrategies(
    openConnection = f(openConnection),
    setAutoCommit = f(setAutoCommit),
    commitConnection = f(commitConnection),
    rollbackConnection = f(rollbackConnection),
    closeConnection = f(closeConnection)
  )

  def noTimeout: ErrorStrategies = all(_.noTimeout)

  def noRetry: ErrorStrategies = all(_.noRetry)

  /** Adds a timeout on all individual operations (open, commit, rollback, etc.). After the timeout expires, a retry
   * may be attempted. */
  def withTimeout(t: Duration): ErrorStrategies = all(_.withTimeout(t))

  /** Timeout on the retrying of all individual operations. When reached, the operation will fail (no more retries). */
  def withRetryTimeout(t: Duration): ErrorStrategies = all(_.withRetryTimeout(t))
}

object ErrorStrategies {
  /** No retries, and no timeout */
  val Nothing: ErrorStrategies = all(ErrorStrategy.Nothing)

  /** No retries, and a 1s timeout. Good as a default for a starting app, as it won't hide potential problems. You'll
   * probably need a more lenient configuration for Production, though. */
  val Brutal: ErrorStrategies = all(ErrorStrategy.Brutal)

  /** Retry forever, with exponential delay (but never more than 10 seconds), no timeout. Good starting point for your
   * Production app, although you should add timeouts. */
  val RetryForever: ErrorStrategies = all(ErrorStrategy.RetryForever)

  object Implicits {
    implicit val Nothing: ErrorStrategies = ErrorStrategies.Nothing
    implicit val Brutal: ErrorStrategies = ErrorStrategies.Brutal
    implicit val RetryForever: ErrorStrategies = ErrorStrategies.RetryForever
  }

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

  def withRetryTimeout(t: Duration): ErrorStrategy = copy(retryTimeout = t)

}

object ErrorStrategy {

  val Nothing: ErrorStrategy =
    ErrorStrategy(Schedule.stop, Duration.Infinity, Duration.Infinity)

  val Brutal: ErrorStrategy =
    ErrorStrategy(Schedule.stop, 1.second, 1.second)

  val RetryForever: ErrorStrategy =
    ErrorStrategy(Schedule.exponential(10.milliseconds) || Schedule.spaced(10.seconds), Duration.Infinity, Duration.Infinity)
}

