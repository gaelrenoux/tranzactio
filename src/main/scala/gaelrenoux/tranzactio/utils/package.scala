package gaelrenoux.tranzactio

import cats.effect.Blocker
import zio.ZIO
import zio.blocking.Blocking
import zio.macros.delegate.Mix

package object utils {

  private class MonomixLeft[A] extends Mix[A, Any] {
    override def mix(a: A, b: Any): A = a
  }

  private class MonomixRight[A] extends Mix[Any, A] {
    override def mix(a: Any, b: A): A = b
  }

  def monomixLeft[A]: A Mix Any = new MonomixLeft[A]

  def monomixRight[A]: Any Mix A = new MonomixRight[A]


  val catsBlocker: ZIO[Blocking, Nothing, Blocker] = ZIO
    .accessM[Blocking](_.blocking.blockingExecutor)
    .map(exe => Blocker.liftExecutionContext(exe.asEC))

}
