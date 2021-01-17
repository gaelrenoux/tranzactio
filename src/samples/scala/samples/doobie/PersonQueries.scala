package samples.doobie

import doobie.implicits._
import io.github.gaelrenoux.tranzactio.DbException
import io.github.gaelrenoux.tranzactio.doobie._
import samples.Person
import zio.stream.ZStream
import zio.{ULayer, ZIO, ZLayer}

object PersonQueries {

  trait Service {
    val setup: TranzactIO[Unit]

    val list: TranzactIO[List[Person]]

    val listStream: TranzactIOStream[Person]

    def insert(p: Person): TranzactIO[Unit]

    val failing: TranzactIO[Unit]
  }

  val live: ULayer[PersonQueries] = ZLayer.succeed(new Service {

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

    val listStream: TranzactIOStream[Person] = tzioStream {
      sql"""SELECT given_name, family_name FROM person""".query[Person].stream
    }

    def insert(p: Person): TranzactIO[Unit] = tzio {
      sql"""INSERT INTO person (given_name, family_name) VALUES (${p.givenName}, ${p.familyName})"""
        .update.run.map(_ => ())
    }

    val failing: TranzactIO[Unit] = tzio {
      sql"""INSERT INTO nonexisting (stuff) VALUES (1)"""
        .update.run.map(_ => ())
    }
  })

  val test: ULayer[PersonQueries] = ZLayer.succeed(new Service {

    val setup: TranzactIO[Unit] = ZIO.succeed(())

    val list: TranzactIO[List[Person]] = ZIO.succeed(Nil)

    val listStream: TranzactIOStream[Person] = ZStream.empty

    def insert(p: Person): TranzactIO[Unit] = ZIO.succeed(())

    val failing: TranzactIO[Unit] = ZIO.fail(DbException.Wrapped(new RuntimeException))
  })

  def setup: ZIO[PersonQueries with Connection, DbException, Unit] = ZIO.accessM(_.get.setup)

  val list: ZIO[PersonQueries with Connection, DbException, List[Person]] = ZIO.accessM(_.get.list)

  val listStream: ZStream[PersonQueries with Connection, DbException, Person] = ZStream.accessStream(_.get.listStream)

  def insert(p: Person): ZIO[PersonQueries with Connection, DbException, Unit] = ZIO.accessM(_.get.insert(p))

  val failing: ZIO[PersonQueries with Connection, DbException, Unit] = ZIO.accessM(_.get.failing)

}

