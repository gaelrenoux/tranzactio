package samples

import zio.Has

package object doobie {
  type PersonQueries = Has[PersonQueries.Service]
}
