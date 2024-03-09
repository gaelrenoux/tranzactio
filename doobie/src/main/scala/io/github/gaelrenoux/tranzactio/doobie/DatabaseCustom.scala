package io.github.gaelrenoux.tranzactio.doobie

import doobie.util.transactor
import io.github.gaelrenoux.tranzactio.{ConnectionSource, DatabaseModuleBase, DbException, ErrorStrategiesRef}
import izumi.reflect.Tag
import zio.{Task, Trace, ZEnvironment, ZIO, ZLayer}

/** @tparam M is a marker type to differentiate custom databases. */
class DatabaseCustom[M: Tag](underlying: Database) extends Database {

  override def transaction[R, E, A](task: => ZIO[Connection with R, E, A], commitOnFailure: => Boolean)
    (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R with Any, Either[DbException, E], A] =
    underlying.transaction[R, E, A](task, commitOnFailure = commitOnFailure)


  override def autoCommit[R, E, A](task: => ZIO[transactor.Transactor[Task] with R, E, A])
    (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R with Any, Either[DbException, E], A] =
    underlying.autoCommit[R, E, A](task)

}

object DatabaseCustom {
  class Module[M: Tag] extends DatabaseModuleBase[Connection, DatabaseCustom[M], DbContext] {
    override def fromConnectionSource(implicit dbContext: DbContext, trace: Trace): ZLayer[ConnectionSource, Nothing, DatabaseCustom[M]] =
      Database.fromConnectionSource.map(env => ZEnvironment(new DatabaseCustom[M](env.get)))
  }

  def apply[M: Tag]: Module[M] = new Module[M]
}
