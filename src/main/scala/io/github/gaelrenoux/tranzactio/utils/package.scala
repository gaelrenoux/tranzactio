package io.github.gaelrenoux.tranzactio

import cats.effect.Blocker
import zio.ZIO
import zio.blocking.Blocking

package object utils {

  val catsBlocker: ZIO[Blocking, Nothing, Blocker] = ZIO
    .access[Blocking](_.get[Blocking.Service].blockingExecutor)
    .map(exe => Blocker.liftExecutionContext(exe.asEC))

}
