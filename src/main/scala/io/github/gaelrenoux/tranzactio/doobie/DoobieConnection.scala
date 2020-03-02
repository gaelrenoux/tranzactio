package io.github.gaelrenoux.tranzactio.doobie

import doobie.util.transactor.Transactor
import zio.Task
import zio.interop.catz._

/** A environment inside which you can run Doobie queries. Needs to be provided by a DoobieDatabase. */
trait DoobieConnection {
  def apply[A](q: Query[A]): Task[A]
}

object DoobieConnection {

  /** LiveConnection: based on a Doobie transactor. */
  class Live private[doobie](transactor: Transactor[Task]) extends DoobieConnection {
    def apply[A](q: Query[A]): Task[A] = transactor.trans.apply(q)
  }

}
