package io.github.gaelrenoux.tranzactio

import zio.stream.ZStream
import zio.{Tag, Trace, ZEnvironment, ZIO, ZLayer}

/**
 * This is a typed database, to use when you have multiple databases in your application. Simply provide a marker type,
 * and ZIO will be able to differentiate between multiple DatabaseT[_] types in your environment.
 * @tparam M Marker type, no instances. */
class DatabaseTBase[M: Tag, Connection](underlying: DatabaseOps.ServiceOps[Connection]) extends DatabaseOps.ServiceOps[Connection] {

  override def transaction[R, E, A](task: => ZIO[Connection with R, E, A], commitOnFailure: => Boolean)
    (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R with Any, Either[DbException, E], A] =
    underlying.transaction[R, E, A](task, commitOnFailure = commitOnFailure)

  override def transactionOrDieStream[R, E, A](stream: => ZStream[Connection with R, E, A], commitOnFailure: => Boolean)
    (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZStream[R with Any, E, A] =
    underlying.transactionOrDieStream[R, E, A](stream, commitOnFailure = commitOnFailure)

  override def autoCommit[R, E, A](task: => ZIO[Connection with R, E, A])
    (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZIO[R with Any, Either[DbException, E], A] =
    underlying.autoCommit[R, E, A](task)

  override def autoCommitStream[R, E, A](stream: => ZStream[Connection with R, E, A])
    (implicit errorStrategies: ErrorStrategiesRef, trace: Trace): ZStream[R with Any, Either[DbException, E], A] =
    underlying.autoCommitStream[R, E, A](stream)
}

object DatabaseTBase {
  trait Companion[Connection, DbContext] {
    type Module[M] = DatabaseTBase.Module[M, Connection, DbContext]

    def apply[M: Tag]: Module[M]
  }

  class Module[M: Tag, Connection: Tag, DbContext](underlying: DatabaseModuleBase[Connection, DatabaseOps.ServiceOps[Connection], DbContext])
    extends DatabaseModuleBase[Connection, DatabaseTBase[M, Connection], DbContext] {
    override def fromConnectionSource(implicit dbContext: DbContext, trace: Trace): ZLayer[ConnectionSource, Nothing, DatabaseTBase[M, Connection]] =
      underlying.fromConnectionSource.map(env => ZEnvironment(new DatabaseTBase[M, Connection](env.get)))
  }
}
