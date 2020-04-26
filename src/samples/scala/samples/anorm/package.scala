package samples

import zio.Has

package object anorm {
  type PersonQueries = Has[PersonQueries.Service]
}
