package io.github.gaelrenoux.tranzactio.doobie

import doobie.util.transactor
import io.github.gaelrenoux.tranzactio.{ConnectionSource, DatabaseModuleBase, DbException, ErrorStrategiesRef}
import izumi.reflect.Tag
import zio.{Task, Trace, ZEnvironment, ZIO, ZLayer}

/**
 * This is a type database, to use when you have multiple databases in your application. Simply provide a marker type,
 * and ZIO will be able to differentiate between multiple DatabaseT[_] types in your environment.
 * @tparam M Marker type, no instances */
class DatabaseT[M: Tag](underlying: Database) extends Database {

  override def transaction[R, E, A](task: => ZIO[Connection with R, E, A], commitOnFailure: => Boolean)
    (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R with Any, Either[DbException, E], A] =
    underlying.transaction[R, E, A](task, commitOnFailure = commitOnFailure)


  override def autoCommit[R, E, A](task: => ZIO[transactor.Transactor[Task] with R, E, A])
    (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R with Any, Either[DbException, E], A] =
    underlying.autoCommit[R, E, A](task)

}

object DatabaseT {
  class Module[M: Tag] extends DatabaseModuleBase[Connection, DatabaseT[M], DbContext] {
    override def fromConnectionSource(implicit dbContext: DbContext, trace: Trace): ZLayer[ConnectionSource, Nothing, DatabaseT[M]] =
      Database.fromConnectionSource.map(env => ZEnvironment(new DatabaseT[M](env.get)))
  }

  def apply[M: Tag]: Module[M] = new Module[M]
}
