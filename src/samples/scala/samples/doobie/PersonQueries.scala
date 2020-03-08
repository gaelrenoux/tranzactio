package samples.doobie

import doobie.implicits._
import io.github.gaelrenoux.tranzactio.DbException
import io.github.gaelrenoux.tranzactio.doobie._
import samples.Person
import zio.ZLayer.NoDeps
import zio.{ZIO, ZLayer}

object PersonQueries {

  trait Service {
    val setup: TranzactIO[Unit]

    val list: TranzactIO[List[Person]]

    def insert(p: Person): TranzactIO[Unit]
  }

  val live: NoDeps[Nothing, PersonQueries] = ZLayer.succeed(new Service {

    val setup: TranzactIO[Unit] = tzio {
      sql"""
        CREATE TABLE person (
          given_name VARCHAR NOT NULL,
          family_name VARCHAR NOT NULL
        )
        """.update.run.map(_ => ())
    }

    val list: TranzactIO[List[Person]] = tzio {
      sql"""SELECT given_name, family_name FROM person""".query[Person].to[List]
    }

    def insert(p: Person): TranzactIO[Unit] = tzio {
      sql"""INSERT INTO person (given_name, family_name) VALUES (${p.givenName}, ${p.familyName})"""
        .update.run.map(_ => ())
    }
  })

  val list: ZIO[PersonQueries with Connection, DbException, List[Person]] = ZIO.accessM(_.get.list)

  def insert(p: Person): ZIO[PersonQueries with Connection, DbException, Unit] = ZIO.accessM(_.get.insert(p))

  def setup: ZIO[PersonQueries with Connection, DbException, Unit] = ZIO.accessM(_.get.setup)

}

