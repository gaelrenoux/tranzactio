import zio.Has

package object samples {
  type Conf = Has[Conf.Root]
}
