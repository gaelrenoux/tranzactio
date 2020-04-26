[TravisCI-Link]: https://travis-ci.org/gaelrenoux/tranzactio
[TravisCI-Badge]: https://travis-ci.org/gaelrenoux/tranzactio.svg?branch=master
[SonatypeReleases-Link]: https://oss.sonatype.org/content/repositories/releases/io/github/gaelrenoux/tranzactio_2.13/
[SonatypeReleases-Badge]: https://img.shields.io/nexus/r/https/oss.sonatype.org/io.github.gaelrenoux/tranzactio_2.13.svg

# TranzactIO

[![Build Status][TravisCI-Badge]][TravisCI-Link]
[![Releases][SonatypeReleases-Badge]][SonatypeReleases-Link]


TranzactIO is a wrapper around some Scala database access libraries (Doobie and Anorm, for now). 
It replaces the library's IO monad by a `ZIO[Connection, E, A]`, or offers it if the library didn't have an IO monad to start with.
When you're done chaining ZIOs and want to execute the transaction, use TranzactIO's Database module to provide a connection for your ZIO.
It can also provide a connection in auto-commit mode, without a transaction.

Any constructive criticism, bug report or offer to help is welcome. Just open an issue or a PR.



# Getting started

## Sbt setup

TranzactIO is available on the Sonatype Central Repository.

In your build.sbt:
```sbt
libraryDependencies += "io.github.gaelrenoux" %% "tranzactio" % version
```


## Wrapping a query

Use `tzio` to wrap Doobie's monad:

```scala
import zio._
import doobie.implicits._
import io.github.gaelrenoux.tranzactio._
import io.github.gaelrenoux.tranzactio.doobie._

val list: ZIO[Connection, DbException, List[String]] = tzio {
    sql"SELECT name FROM users".query[String].to[List]
}
```

For Anorm, use `tzio` to provide the connection you need. The operation will be wrapped in a ZIO.
```scala
import zio._
import anorm._
import io.github.gaelrenoux.tranzactio._
import io.github.gaelrenoux.tranzactio.anorm._

val list: ZIO[Connection, DbException, List[String]] = tzio { implicit c =>
    SQL"SELECT name FROM users".as(SqlParser.str(1).*)
}
```

Type `Connection` is **not** Java's `java.sql.Connection`. It is the type associated to a ZIO module specific to the library you're wrapping around. You'll need the associated `Database` module to provide a `Connection`.

Type `TranzactIO[A]` is an alias for `ZIO[Connection, DbException, A]` (`DbException` is generic and not linked to a specific library).


## Running the transaction

Use the Database module to provide the connection. You'll have to provide the connection source.

```scala
import io.github.gaelrenoux.tranzactio._
import io.github.gaelrenoux.tranzactio.doobie._
import javax.sql.DataSource
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console

object MyApp {

  val datasource: DataSource = ??? // typically from a connection pool, like HikariCP

  val dbLayer: ZLayer[Blocking with Clock, Nothing, Database] = Database.fromDatasource(datasource)

  val zio1: ZIO[Connection with Console, Exception, List[String]] = ???
  val result1: ZIO[Database with Console, Either[DbException, Exception], List[String]] = Database.transactionR[Console](zio1)
  // Connection exceptions are Left

  val zio2: ZIO[Connection, Exception, List[String]] = ???
  val result2: ZIO[Database, Exception, List[String]] = Database.transactionOrWiden(zio2)
  // Exception is widened, no Either.
  // transactionOrWiden instead on transactionOrWidenR since there is no additional environment (apart from the Connection)

  val zio3: ZIO[Connection, String, List[String]] = ???
  val result3: ZIO[Database, String, List[String]] = Database.autoCommitOrDie(zio3)
  // Connection exceptions are transformed into defects.
  // Also, no transaction here, we are using auto-commit mode
}
```

Code is identical for Anorm, except for the import: `io.github.gaelrenoux.tranzactio.anorm._` instead of `io.github.gaelrenoux.tranzactio.doobie._`.

You'll notice that all methods exists in two variants, with or without the final `R`.
The final `R` denotes cases where there is additional environment requirements on the query ZIO, not just a `Connection`.
When using an `R` method, you need to specify the additional environment as type parameter (Scala's compiler is not smart enough to infer it correctly on its own).

Check in `src/main/samples` for more samples.


## Error handling

TranzactIO has no specific error handling for errors that happens when running the queries (query errors).
Since you have direct access to the ZIO instance representing that query, it's up to you to add timeouts or retries, recover from errors, etc.
However, you do not have direct access to the effects relating to connection management (opening and closing, committing and rollbacking), and therefore cannot handle directly the associated errors (connection errors).

To set up the retries and timeouts on connection errors, you can pass an `ErrorStrategies` instance when creating the `Database` layer.
It defines the error strategy on every operation done by the `Database` module.

In addition, there exists a number of commodity methods on `Database` to let you chose how unrecovered connection errors
(that were not recovered through the retries defined in the `ErrorStrategies`) should be handled in the resulting ZIO instance,
while preserving possible errors in the initial ZIO (query errors):
- With `transaction[R]`, the error is an `Either`: `Right` wraps a query error, and `Left` wraps a connection error.
- With `transactionOrDie[R]`, connection errors are considered as defects, and do not appear in the type signature.
- With `transactionOrWiden[R]`, the query error type will be widened to encompass `DbException`,
and connection errors will appear as normal ZIO errors.
This is especially useful if your query error type is already `DbException` or a parent type for `DbException` (like `Exception` in the example above).

The same commodity variants exist for `autoCommit`.
 
 


# Why ?

On my applications, I regularly have quite a bunch of business logics around my queries.
If I want to run that logic within a transaction, I had to wrap it with Doobie's ConnectionIO.
But I'm already using ZIO as my effect monad! I don't want another one...
In addition, IO monads on DB libraries (like Doobie's `ConnectionIO`) misses quite a bit of the operations that ZIO has.

That's where TranzactIO comes from. I wanted a way to use ZIO everywhere, and run the transaction whenever I decided.



# What's next


## API cleanup

I'm still figuring out what's the best API for that library.
I'm pretty happy with how it is right now, but I feel like there's still room for improvement.


## More wrappers

I want to add wrappers around more database access libraries.
Anorm was the second one I did, next should probably be Quill (based on the popularity of the project on GitHub),
but I'm completely unfamiliar with it.

Slick, however, is a problem. I know it quite well, tried to implement a TranzactIO module for it, and couldn't.
Transactions cannot be handled externally using Slick.
I don't think it's doable until this ticket is done: https://github.com/slick/slick/issues/1563
