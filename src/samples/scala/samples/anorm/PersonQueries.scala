package samples.anorm

import anorm._
import io.github.gaelrenoux.tranzactio.DbException
import io.github.gaelrenoux.tranzactio.anorm._
import samples.Person
import zio.{ULayer, ZIO, ZLayer}

object PersonQueries {

  private val personParser: RowParser[Person] = Macro.namedParser[Person]

  trait Service {
    val setup: TranzactIO[Boolean]

    val list: TranzactIO[List[Person]]

    def insert(p: Person): TranzactIO[Boolean]

    val failing: TranzactIO[Int]
  }

  val live: ULayer[PersonQueries] = ZLayer.succeed(new Service {

    val setup: TranzactIO[Boolean] = tzio { implicit c =>
      SQL"""
        CREATE TABLE person (
          given_name VARCHAR NOT NULL,
          family_name VARCHAR NOT NULL
        )
        """.execute()
    }

    val list: TranzactIO[List[Person]] = tzio { implicit c =>
      SQL"""SELECT given_name as givenName, family_name as familyName FROM person""".as(personParser.*)
    }

    def insert(p: Person): TranzactIO[Boolean] = tzio { implicit c =>
      SQL"""INSERT INTO person (given_name, family_name) VALUES (${p.givenName}, ${p.familyName})""".execute()
    }

    val failing: TranzactIO[Int] = tzio { implicit c =>
      SQL"""INSERT INTO nonexisting (stuff) VALUES (1)""".executeUpdate()
    }
  })

  val test: ULayer[PersonQueries] = ZLayer.succeed(new Service {

    val setup: TranzactIO[Boolean] = ZIO.succeed(true)

    val list: TranzactIO[List[Person]] = ZIO.succeed(Nil)

    def insert(p: Person): TranzactIO[Boolean] = ZIO.succeed(true)

    val failing: TranzactIO[Int] = ZIO.fail(DbException.Wrapped(new RuntimeException))
  })

  def setup: ZIO[PersonQueries with Connection, DbException, Boolean] = ZIO.accessM(_.get.setup)

  val list: ZIO[PersonQueries with Connection, DbException, List[Person]] = ZIO.accessM(_.get.list)

  def insert(p: Person): ZIO[PersonQueries with Connection, DbException, Boolean] = ZIO.accessM(_.get.insert(p))

  val failing: ZIO[PersonQueries with Connection, DbException, Int] = ZIO.accessM(_.get.failing)

}

