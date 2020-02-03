package gaelrenoux.tranzactio.slick

import _root_.slick.basic.BasicBackend
import zio.Task

/** A environment inside which you can run Doobie queries. Needs to be provided by a DoobieDatabase. */
trait SlickConnection {
  def apply[A](q: Query[A]): Task[A]
}

object SlickConnection {

  /** LiveConnection: based on a Slick database transactor. */
  class Live private[doobie](db: BasicBackend#DatabaseDef) extends SlickConnection {
    def apply[A](q: Query[A]): Task[A] = Task.fromFuture(_ => db.run(q)) // TODO not using the EC here
  }


}
