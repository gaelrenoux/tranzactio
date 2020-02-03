package samples.doobie

import doobie.implicits._
import gaelrenoux.tranzactio.doobie._
import samples.Person


trait PersonQueries {
  val personQueries: PersonQueries.Service[Any]
}

object PersonQueries {

  trait Service[R] {
    val list: TranzactIO[List[Person]]

    def insert(p: Person): TranzactIO[Unit]
  }

  trait Live extends PersonQueries {

    override lazy val personQueries: Service[Any] = new Service[Any] {

      val list: TranzactIO[List[Person]] = tzio {
        sql"""SELECT given_name, family_name FROM users""".query[Person].to[List]
      }

      def insert(p: Person): TranzactIO[Unit] = tzio {
        sql"""INSERT INTO person (given_name, family_name) VALUES (${p.givenName}, ${p.familyName})"""
          .update.run.map(_ => ())
      }
    }
  }

}

