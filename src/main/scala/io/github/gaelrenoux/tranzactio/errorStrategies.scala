package io.github.gaelrenoux.tranzactio

import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.{Schedule, ZIO}

/** How to handle issues in the various operations of the database. Note that this only applies to the operation
 * performed when handling the connection, and not the execution of the requests! */
sealed trait ErrorStrategiesRef {
  def orElse(es: ErrorStrategiesRef): ErrorStrategiesRef

  val orElseDefault: ErrorStrategies

  def ref: ErrorStrategiesRef = this
}



/** Refer to the parent module, up to the default value in Tranzactio (which is Brutal). */
case object ErrorStrategiesParent extends ErrorStrategiesRef {
  override def orElse(es: ErrorStrategiesRef): ErrorStrategiesRef = es

  override val orElseDefault: ErrorStrategies = ErrorStrategies.Nothing
}



/** Contains one ErrorStrategy for each DB operation. Carries a few methods to apply changes to all methods.
 * @param openConnection Note that `openConnection` is a special case. Timeouts should '''not''' be handled in ZIO over
 *                       this method, as that could lead to connection leaks. Therefore, the timeout method specifically
 *                       ignore `openConnection`.
 */
case class ErrorStrategies(
    openConnection: ErrorStrategy,
    setAutoCommit: ErrorStrategy,
    commitConnection: ErrorStrategy,
    rollbackConnection: ErrorStrategy,
    closeConnection: ErrorStrategy
) extends ErrorStrategiesRef {

  override def orElse(es: ErrorStrategiesRef): ErrorStrategies = this

  override val orElseDefault: ErrorStrategies = this

  private def all(f: ErrorStrategy => ErrorStrategy, applyToOpenConnection: Boolean = true): ErrorStrategies = ErrorStrategies(
    openConnection = if (applyToOpenConnection) f(openConnection) else openConnection,
    setAutoCommit = f(setAutoCommit),
    commitConnection = f(commitConnection),
    rollbackConnection = f(rollbackConnection),
    closeConnection = f(closeConnection)
  )

  /** Adds a timeout on all individual operations (commit, rollback, etc.).
   *
   * No timeout will be applied to openConnection, as this would cause leaking connections. If for some reason you want
   * a timeout on openConnection, you need to set the ErrorStrategy for openConnection manually.
   */
  def timeout(d: Duration): ErrorStrategies =
    all(_.timeout(d), applyToOpenConnection = false)

  def retry(schedule: Schedule[Clock, Any, Any]): ErrorStrategies =
    all(_.retry(schedule))

  def retryCountExponential(count: Int, delay: Duration, factor: Double = 2.0, maxDelay: Duration = Duration.Infinity): ErrorStrategies =
    all(_.retryCountExponential(count, delay, factor, maxDelay))

  def retryCountFixed(count: Int, delay: Duration): ErrorStrategies =
    all(_.retryCountFixed(count, delay))

  def retryForeverExponential(delay: Duration, factor: Double = 2.0, maxDelay: Duration = Duration.Infinity): ErrorStrategies =
    all(_.retryForeverExponential(delay, factor, maxDelay))

  def retryForeverFixed(delay: Duration): ErrorStrategies =
    all(_.retryForeverFixed(delay))
}

/** The ErrorStrategies companion object is the starting point to define an ErrorStrategies, and therefore is defined as
 * a set of empty strategies (no timeout and no retry). */
object ErrorStrategies extends ErrorStrategies(ErrorStrategy, ErrorStrategy, ErrorStrategy, ErrorStrategy, ErrorStrategy) {

  /** Alias for the ErrorStrategies companion object. Can be used for clarity, to mark when you actually want no retry and no timeout. */
  val Nothing: ErrorStrategies = this

  /** No concrete strategies. Uses the parent module's strategies. */
  val Parent: ErrorStrategiesParent.type = ErrorStrategiesParent // Must be defined after Nothing, as ErrorStrategiesParent uses it

  /** Implicit ErrorStrategies, to be imported when needed. */
  object Implicits {
    implicit val Parent: ErrorStrategiesParent.type = ErrorStrategies.Parent
    implicit val Nothing: ErrorStrategies = ErrorStrategies.Nothing
  }

}



/** An ErrorStrategy defines how to handle one of the DB operations (openConnection, commit, etc.) It is typically
 * created starting from the ErrorStrategy object, then applying timeout and retry. */
trait ErrorStrategy {
  self =>

  /** How this ErrorStrategy transforms a DB operation. */
  def apply[R, A](z: ZIO[R, DbException, A]): ZIO[R with Clock with Blocking, DbException, A]

  /** Adds a timeout to the current ErrorStrategy. Note that if a retry has already been defined, the timeout is applied
   * '''after''' the retry. */
  def timeout(d: Duration): ErrorStrategy = new ErrorStrategy {
    override def apply[R, A](z: ZIO[R, DbException, A]): ZIO[R with Clock with Blocking, DbException, A] =
      self(z).timeoutFail(DbException.Timeout(d))(d)
  }

  /** Adds a retry to the current ErrorStrategy. */
  def retry(schedule: Schedule[Clock, Any, Any]): ErrorStrategy = new ErrorStrategy {
    def apply[R, A](z: ZIO[R, DbException, A]): ZIO[R with Clock with Blocking, DbException, A] =
      self(z).retry(schedule)
  }

  def retryCountExponential(count: Int, delay: Duration, factor: Double = 2.0, maxDelay: Duration = Duration.Infinity): ErrorStrategy = {
    if (maxDelay == Duration.Infinity) retry(Schedule.recurs(count) && Schedule.exponential(delay, factor))
    else retry(Schedule.recurs(count) && (Schedule.exponential(delay, factor) || Schedule.spaced(10.seconds)))
  }

  def retryCountFixed(count: Int, delay: Duration): ErrorStrategy = {
    retry(Schedule.recurs(count) && Schedule.spaced(delay))
  }

  def retryForeverExponential(delay: Duration, factor: Double = 2.0, maxDelay: Duration = Duration.Infinity): ErrorStrategy = {
    if (maxDelay == Duration.Infinity) retry(Schedule.exponential(delay, factor))
    else retry(Schedule.exponential(delay, factor) || Schedule.spaced(10.seconds))
  }

  def retryForeverFixed(delay: Duration): ErrorStrategy = {
    retry(Schedule.spaced(delay))
  }
}

/** The ErrorStrategy companion object is the starting point to define an ErrorStrategy, and therefore is defined as an
 * empty strategy (no timeout and no retry). */
object ErrorStrategy extends ErrorStrategy {

  override def apply[R, A](z: ZIO[R, DbException, A]): ZIO[R with Clock with Blocking, DbException, A] = z

  /** Alias for the ErrorStrategy companion object. Can be used for clarity, to mark when you actually want no retry and no timeout. */
  val Nothing: ErrorStrategy = this
}

