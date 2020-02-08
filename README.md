TranzactIO is a wrapper around some Scala database access library (only Doobie for now). It replaces the library's IO monad by a `ZIO[Connection, E, A]`.
When you're done chaining ZIOs and want to execute the transaction, use TranzactIO's Database module to provide a connection for your ZIO.
It can also provide a connection in auto-commit mode, without a transaction.

**Warning**: This is still a very early alpha version. API is still fluid and might change without notice. 
It's not on any Maven repository, so you'll have to clone it and build it yourself (just do `sbt publishLocal`).

# Setup

In your build.sbt:
```sbt
libraryDependencies += "gaelrenoux" %% "tranzactio" % "0.1-SNAPSHOT"
```

# Doobie

## Wrapping a Doobie query
```scala
import zio._
import doobie.implicits._
import gaelrenoux.tranzactio._
import gaelrenoux.tranzactio.doobie._

val list: ZIO[Connection, DbException, List[String]] = tzio {
    sql"""SELECT name FROM users""".query[String].to[List]
}
```

Type `Connection` is actually an alias for `DoobieConnection`: you'll need a `DoobieDatabase` to provide it.

Type `TranzactIO[A]` is an alias for `ZIO[Connection, DbException, List[String]]`

## Running the transaction

```scala
import zio._
import gaelrenoux.tranzactio._
import gaelrenoux.tranzactio.doobie._
import javax.sql.DataSource

trait MyApp extends Database with ConnectionSource.FromDatasource {

  override val datasource: DataSource = ??? // typically from a connection pool, like HikariCP

  val zio1: ZIO[Connection with console.Console, Exception, List[String]] = ???
  val result1: ZIO[console.Console, Either[DbException, Exception], List[String]] = database.transaction(zio1)
  // Connection exceptions are Left

  val zio2: ZIO[Connection, Exception, List[String]] = ???
  val result2: IO[Exception, List[String]] = database.transactionOrWiden(zio2)
  // Exception is widened, no Either. Also, no environment since zio2 only needed the Connection

  val zio3: ZIO[Connection, String, List[String]] = ???
  val result3: IO[String, List[String]] = database.transactionOrDie(zio3)
  // Connection exceptions are transformed into defects.

}
```

To get the database module directly, there are also a few commodity methods:
```scala
import zio.ZIO
import gaelrenoux.tranzactio._
import gaelrenoux.tranzactio.doobie._
import javax.sql.DataSource

val ds: DataSource = ???
val db: Database.Live = Database.fromDatasource(ds)

val integrationTestDb: Database.Live = Database.fromDriverManager(
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:54320/",
    "login",
    "password"
)
// Don't use that for production: connections will be opened and closed or each transaction.
```

# What's next

## More wrappers
I want to add wrappers around more database access libraries. I'm looking into Quill right now, I'll probably take a look at Anorm at some point as well.

Slick, however, is a problem. I tried it, and given the way transactions are handled in Slick I don't think it's doable until this ticket is fixed: https://github.com/slick/slick/issues/1563
