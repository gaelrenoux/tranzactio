package io.github.gaelrenoux.tranzactio.doobie

import java.sql.{Connection => SqlConnection}

import cats.effect.Resource
import doobie.free.KleisliInterpreter
import doobie.util.transactor.{Strategy, Transactor}
import io.github.gaelrenoux.tranzactio.utils.catsBlocker
import zio.blocking.Blocking
import zio.interop.catz._
import zio.{Has, Task, ZIO}

/** A environment inside which you can run Doobie queries. Needs to be provided by a DoobieDatabase. */
object DoobieConnection {

  trait Service {
    def apply[A](q: Query[A]): Task[A]
  }

  /** LiveConnection: based on a Doobie transactor. */
  class ServiceLive private[doobie](transactor: Transactor[Task]) extends DoobieConnection.Service {
    def apply[A](q: Query[A]): Task[A] = transactor.trans.apply(q)
  }

  def fromSqlConnection(connection: SqlConnection): ZIO[Blocking, Nothing, Connection] = catsBlocker.map { b =>
    val connect = (c: SqlConnection) => Resource.pure[Task, SqlConnection](c)
    val interp = KleisliInterpreter[Task](b).ConnectionInterpreter
    val doobieTransactor = Transactor(connection, connect, interp, Strategy.void)
    new DoobieConnection.ServiceLive(doobieTransactor)
  }.map(Has(_))

}
