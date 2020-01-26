package gaelrenoux.tranzactio

import zio.ZIO

trait Module {
  type Database
  type Connection
  type Query[A]

  type TranzactIO[A] = ZIO[Connection, DbException, A]

  def tzio[A](q: Query[A]): TranzactIO[A]

}
