package samples.slick

import samples.Person
import slick.jdbc.H2Profile.api._

object PersonQueries {

  class Persons(tag: Tag) extends Table[(Int, String, String)](tag, "person") {
    def id = column[Int]("SUP_ID", O.PrimaryKey) // This is the primary key column
    def givenName = column[String]("given_name")

    def familyName = column[String]("family_name")

    def * = (id, givenName, familyName)
  }

  val persons = TableQuery[Persons]

  val list: DBIO[Seq[(String, String)]] = {
    sql"""SELECT given_name, family_name FROM users""".as[(String, String)]
  }

  def insert(p: Person): DBIO[Int] = {
    sqlu"""INSERT INTO person (given_name, family_name) VALUES (${p.givenName}, ${p.familyName})"""
  }

}

