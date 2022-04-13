package io.github.gaelrenoux


import zio.Clock

package object tranzactio {

  type ConnectionSource = ConnectionSource.Service

  type TranzactioEnv = Clock

}
