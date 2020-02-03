package gaelrenoux.tranzactio

import zio.ZIO

package object slick extends Module {
  override type Database = SlickDatabase
  override type Connection = SlickConnection
  override type Query[A] = _root_.slick.dbio.DBIO[A]

  val Database: SlickDatabase.type = SlickDatabase
  val Connection: SlickConnection.type = SlickConnection

  type SDatabase = _root_.slick.basic.BasicBackend#Database

  override def tzio[A](q: Query[A]): TranzactIO[A] = ZIO.accessM[SlickConnection](_.apply(q)).mapError(DbException)

}
