package io.github.gaelrenoux

import zio.Has

package object tranzactio {

  type ConnectionSource = Has[ConnectionSource.Service]

}
