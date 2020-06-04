package io.github.gaelrenoux.tranzactio

import cats.effect.Blocker
import zio.ZIO
import zio.blocking.Blocking

package object utils {

  val ZCatsBlocker: ZIO[Blocking, Nothing, Blocker] = ZIO.access[Blocking] { b =>
    Blocker.liftExecutionContext(b.get.blockingExecutor.asEC)
  }

}
