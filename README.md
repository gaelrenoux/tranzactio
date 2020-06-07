[TravisCI-Link]: https://travis-ci.org/gaelrenoux/tranzactio
[TravisCI-Badge]: https://travis-ci.org/gaelrenoux/tranzactio.svg?branch=master
[SonatypeReleases-Link]: https://oss.sonatype.org/content/repositories/releases/io/github/gaelrenoux/tranzactio_2.13/
[SonatypeReleases-Badge]: https://img.shields.io/nexus/r/https/oss.sonatype.org/io.github.gaelrenoux/tranzactio_2.13.svg

# TranzactIO

[![Build Status][TravisCI-Badge]][TravisCI-Link]
[![Releases][SonatypeReleases-Badge]][SonatypeReleases-Link]


TranzactIO is a ZIO wrapper for some Scala database access libraries (Doobie and Anorm, for now).

If the library comes with an IO monad (like Doobie's `ConnectionIO`), it lifts it into a `ZIO[Connection, E, A]`.
If the library doesn't have an IO monad to start with (like Anorm), it a `ZIO[Connection, E, A]` for the role.

When you're done chaining ZIOs and want to execute the transaction, use TranzactIO's `Database` module to provide a connection for your ZIO.
It can also provide a connection in auto-commit mode, without a transaction.

It comes with a very small amount of dependencies: only ZIO and ZIO-interop-Cats are required.

Any constructive criticism, bug report or offer to help is welcome. Just open an issue or a PR.



### Why ?

On my applications, I regularly have quite a bunch of business logics around my queries.
If I want to run that logic within a transaction, I had to wrap it with Doobie's `ConnectionIO`.
But I'm already using ZIO as my effect monad! I don't want another one...
In addition, IO monads on DB libraries (like Doobie's `ConnectionIO`) misses quite a bit of the operations that ZIO has.

That's where TranzactIO comes from. I wanted a way to use ZIO everywhere, and run the transaction whenever I decided.





## Getting started


### Sbt setup

TranzactIO is available on the Sonatype Central Repository (see the Nexus badge on top of this README to get the version number). In your build.sbt:
```sbt
libraryDependencies += "io.github.gaelrenoux" %% "tranzactio" % TranzactIOVersion
```

In addition, you will need to declare the database access lbrary you are using. For instance with Doobie:
```sbt
libraryDependencies += "org.tpolecat" %% "doobie-core" % DoobieVersion
```



### Imports

Most of the time, you will need to import two packages. The first is `io.github.gaelrenoux.tranzactio._` and contains Tranzactio's generic classes.

The second one is specific to a library, and contains the associated elements. The names of most elements in those packages are the same in each package, for instance the `tzio` function, or the `Connection` and `Database` classes. The package is always named after the library it is used with, e.g.:
- `io.github.gaelrenoux.tranzactio.doobie._`
- `io.github.gaelrenoux.tranzactio.anorm._`



### Wrapping a query

Just use `tzio` to wrap your usual query type!

Note that `Connection` is **not** Java's `java.sql.Connection`, it's a TranzactIO library-specific type.
`DbException` is generic (not library-specific), and represents any error in relation with the DB.

#### Doobie

```scala
import zio._
import doobie.implicits._
import io.github.gaelrenoux.tranzactio._
import io.github.gaelrenoux.tranzactio.doobie._

val list: ZIO[Connection, DbException, List[String]] = tzio {
    sql"SELECT name FROM users".query[String].to[List]
}
```


#### Anorm

Since Anorm doesn't provide an IO monad (or even a specific query type), `tzio` will provide the connection you need to run a query. The operation will be wrapped in a ZIO (as a blocking effect).

```scala
import zio._
import anorm._
import io.github.gaelrenoux.tranzactio._
import io.github.gaelrenoux.tranzactio.anorm._

val list: ZIO[Connection, DbException, List[String]] = tzio { implicit c =>
    SQL"SELECT name FROM users".as(SqlParser.str(1).*)
}
```



### Running the transaction (or using auto-commit)

The `Database` module from the same library-specific package provides the method needed to run the queries.

Here are some examples with Doobie.
The code for Anorm is identical, except it has a different import: `io.github.gaelrenoux.tranzactio.anorm._` instead of `io.github.gaelrenoux.tranzactio.doobie._`.

```scala
import io.github.gaelrenoux.tranzactio._
import io.github.gaelrenoux.tranzactio.doobie._
import zio._
import zio.console.Console

// Let's start with a very simple one. Connection exceptions are transformed into defects.
val zio: ZIO[Connection, String, Long] = ???
val simple: ZIO[Database, String, Long] = Database.transactionOrDie(zio)

// If you have an additional environment, use the ***R method. The environment type must be specified.
val zioEnv: ZIO[Connection with Console, String, Long] = ???
val withEnv: ZIO[Database with Console, String, Long] = Database.transactionOrDieR[Console](zioEnv)

// Do you want to handle connection errors yourself? They will appear on the Left side of the Either.
val withSeparateErrors: ZIO[Database, Either[DbException, String], Long] = Database.transaction(zio)

// Is the only error you are expecting coming from the DB ? Let's handle all of them at the same time.
val zioDbEx: ZIO[Connection, DbException, Long] = ???
val withDbEx: ZIO[Database, DbException, Long] = Database.transactionOrWiden(zioDbEx)

// Or maybe you're just grouping all errors together as exceptions.
val zioEx: ZIO[Connection, java.io.IOException, Long] = ???
val withEx: ZIO[Database, Exception, Long] = Database.transactionOrWiden(zioEx)

// And if you're actually not interested in a transaction, you can just auto-commit all queries.
val zioAutoCommit: ZIO[Database, String, Long] = Database.autoCommitOrDie(zio)
```



### Providing the Database

The resulting ZIO requires a `Database` as an environment, that will be provided through a `ZLayer`.

The `Database` object lets you construct from a `DataSource` a `ZLayer` for `Database`. Nost connection pool implementations (like HikariCP) provide a `DataSource` representation.
You can also get a `ZLayer` directly from the DB url and credentials, using Java's DriverManager, but that's not recommended outside of a test environment.

Again, the code for Anorm is identical, except it has a different import: `io.github.gaelrenoux.tranzactio.anorm._` instead of `io.github.gaelrenoux.tranzactio.doobie._`.

```scala
import io.github.gaelrenoux.tranzactio._
import io.github.gaelrenoux.tranzactio.doobie._
import javax.sql.DataSource
import zio._
import zio.blocking.Blocking
import zio.clock.Clock

// You can get a Database ZLayer directly from the Java DriverManager for testing.
val dbLayerFromUrl: ZLayer[Blocking with Clock, Nothing, Database] = Database.fromDriverManager("jdbc:h2:mem:samble-anorm-app;DB_CLOSE_DELAY=10", "sa", "sa")

// Get a Datasource from the connection pool, then create the layer. If the Datasource is a layer itself, just check ZIO's documentation on how to chain layers.
val datasource: DataSource = ???
val dbLayerFromDatasource: ZLayer[Blocking with Clock, Nothing, Database] = Database.fromDatasource(datasource)
```



### More code samples
Find more in `src/main/samples`, or look below for some details.





## More information

### Version compatibility

The table below indicates for each version of TranzactIO, the versions of ZIO or libraries it's been built with. Check the backward compatibily information on those libraries to check if your version is supported with a particular version of TranzactIO.

| TranzactIO | ZIO          | Doobie       | Anorm        |
|------------|--------------|--------------|--------------|
| 0.1.0      | 1.0.0-RC17   | 0.8.6        | -            |
| 0.2.0      | 1.0.0-RC18-2 | 0.8.6        | -            |
| 0.3.0      | 1.0.0-RC18-2 | 0.8.6        | 2.6.5        |
| 0.4.0      | 1.0.0-RC19-2 | 0.9.0        | 2.6.5        |
| master     | 1.0.0-RC20   | 0.9.0        | 2.6.5        |



### Error definitions

We'll talk a bit about errors, so here are two definitions. TranzactIO identifies two categories of errors relating to the DB: query errors, and connection errors:

**Query errors** happen when you run a specific query.
They can be timeouts, SQL syntax errors, constraint errors, etc.
When you have a `ZIO[Connection, E, A]`, E is the type for query errors.

**Connection errors** happen when you manage connections or transactions: opening connections, creating, commiting or rollbacking transactions, etc.
They are not linked to a specific query.
They are always reported as a `DbException`.



### Database methods

There are two families of operations on the `Database` class: `transaction` and `autoCommit`. I'll only describe `transaction` here, keep in mind that there's an identical set of operations with `autoCommit` instead.

 When providing the transaction with `Database`, you have three variants of the `transaction` method, which will handle unrecovered connection errors.
- With `transaction`, the resulting error type is an `Either`: `Right` wraps a query error, and `Left` wraps a connection error. This is the most generic method, leaving you to handle all errors how you see fit.
- With `transactionOrDie`, connection errors are considered as defects, and do not appear in the type signature.
- With `transactionOrWiden`, the resulting error type will be the closest supertype of the query error type and `DbException`, and the error in the result may be a query error or a connection error. This is especially useful if your query error type is already `DbException` or directly `Exception`, as in the examples above.

```scala
val zio: ZIO[Connection, E, A] = ???
val result1: ZIO[Database, Either[DbException, E], A] = Database.transaction(zio)
val result2: ZIO[Database, E, A] = Database.transactionOrDie(zio)
// assuming E extends Exception:
val result3: ZIO[Database, Exception, A] = Database.transactionOrWiden(zio)
```
 
In addition, a frequent case is to have an additional environment on your ZIO monad, e.g.: `ZIO[ZEnv with Connection, E, A]`.
To handle this case, all methods mentioned above have an additional variant with a final `R`.
 
When using an `***R` method, you will need to provide the additional environment type as a type parameter (Scala's compiler is not smart enough to infer it correctly on its own):
```scala
val zio: ZIO[ZEnv with Connection, E, A] = ???
val result1: ZIO[Database with ZEnv, Either[DbException, E], A] = Database.transactionR[ZEnv](zio)
val result2: ZIO[Database with ZEnv, E, A] = Database.transactionOrDieR[ZEnv](zio)
// assuming E extends Exception:
val result3: ZIO[Database with ZEnv, Exception, A] = Database.transactionOrWidenR[ZEnv](zio) 
```



### Database module configuration

#### Error handling

TranzactIO has no specific error handling for query errors.
Since you, as the developer, have direct access to the ZIO instance, it's up to you to add timeouts or retries, recover from errors, etc.
However, you do not have access to the connection errors, which are hidden in the `Database` module. 

To set up the error handling on connection errors, you need to pass an `ErrorStrategies` instance when creating the `Database` layer.
It defines the error strategy on every operation done by the `Database` module.

```scala
val dbLayerFromDatasource: ZLayer[Blocking with Clock, Nothing, Database] =
    Database.fromDatasource(datasource, ErrorStrategies.RetryForever)
```

If none is provided, the default is `ErrorStrategies.Brutal`. It is an unforgiving setting, with no retry and 1s timeout on all operations.
Check on `ErrorStrategies` the available possibilities.
A typical configuration for production would be to start with `ErrorStrategies.RetryForever` and add timeouts, using the `withTimeout` and `withRetryTimeout` methods. 

You can also construct manually an `ErrorStrategies` instance, setting different policies for each action (opening connections, committing, rollbacking, etc.)





## What's next ?

### Evolve the API

The API is pretty final by now on the main points (like the use of `tzio` and `Database` methods).
However, the part regarding the construction of layers might change depending on ZIO's changes on them.



### More database access libraries

I want to add wrappers around more database access libraries.
Anorm was the second one I did, next should probably be Quill (based on the popularity of the project on GitHub),
but I'm completely unfamiliar with it.

Slick, however, is a problem. I know it quite well, tried to implement a TranzactIO module for it, and couldn't.
Transactions cannot be handled externally using Slick.
I don't think it's doable until this ticket is done: https://github.com/slick/slick/issues/1563
