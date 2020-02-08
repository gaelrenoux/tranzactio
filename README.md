TranzactIO is a wrapper around some Scala database access library (only Doobie for now). It replaces the library's IO monad by a `ZIO[Connection, E, A]`.
When you're done chaining ZIOs and want to execute the transaction, use TranzactIO's Database module to provide a connection for your ZIO.
It can also provide a connection in auto-commit mode, without a transaction.

**Warning**: This is still a very early alpha version. API is still fluid and might change without notice. 
It's not on any Maven repository, so you'll have to clone it and build it yourself (just do `sbt publishLocal`).

Any constructive criticism, bug report or offer to help is welcome. Just open an issue or a PR.


# Getting started

## Sbt setup

In your build.sbt:
```sbt
libraryDependencies += "gaelrenoux" %% "tranzactio" % "0.1-SNAPSHOT"
```


## Wrapping a query
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
  val result1: ZIO[console.Console, Either[DbException, Exception], List[String]] = database.transactionR(zio1)
  // Connection exceptions are Left

  val zio2: ZIO[Connection, Exception, List[String]] = ???
  val result2: IO[Exception, List[String]] = database.transactionOrWiden(zio2)
  // Exception is widened, no Either. Also, no environment since zio2 only needed the Connection

  val zio3: ZIO[Connection, String, List[String]] = ???
  val result3: IO[String, List[String]] = database.transactionOrDieR(zio3)
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


# Why ?

On my applications, I regularly have quite a bunch of business logics around my queries.
If I want to run that logic within a transaction, I had to wrap it with Doobie's ConnectionIO.
But I'm already using ZIO as my effect monad! I don't want another one...
In addition, ConnectionIO misses quite a bit of the operations that ZIO has.

That's where TranzactIO comes from. I wanted a way to use ZIO everywhere, and run the transaction whenever I decided.


# What's next

## More wrappers
I want to add wrappers around more database access libraries. I'm looking into Quill right now, I'll probably take a look at Anorm at some point as well.

Slick, however, is a problem. I tried it, and given the way transactions are handled in Slick I don't think it's doable until this ticket is fixed: https://github.com/slick/slick/issues/1563
