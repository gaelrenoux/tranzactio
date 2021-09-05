package io.github.gaelrenoux

import zio.Has
import zio.blocking.Blocking
import zio.clock.Clock

package object tranzactio {

  type ConnectionSource = Has[ConnectionSource.Service]

  type TranzactioEnv = Blocking with Clock

}
