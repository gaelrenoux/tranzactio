[CI-Badge]: https://github.com/gaelrenoux/tranzactio/actions/workflows/ci.yml/badge.svg
[CI-Link]: https://github.com/gaelrenoux/tranzactio/actions?query=branch%3Amaster
[SonatypeReleases-Link]: https://oss.sonatype.org/content/repositories/releases/io/github/gaelrenoux/tranzactio_2.13/
[SonatypeReleases-Badge]: https://img.shields.io/nexus/r/https/oss.sonatype.org/io.github.gaelrenoux/tranzactio_2.13.svg

# TranzactIO

[![CI][CI-Badge]][CI-Link]
[![Releases][SonatypeReleases-Badge]][SonatypeReleases-Link]


TranzactIO is a ZIO wrapper for some Scala database access libraries (Doobie and Anorm, for now).

If the library comes with an IO monad (like Doobie's `ConnectionIO`), it lifts it into a `ZIO[Connection, E, A]`.
If the library doesn't have an IO monad to start with (like Anorm), it provides a `ZIO[Connection, E, A]` for the role.

Note that `Connection` is **not** Java's `java.sql.Connection`, it's a TranzactIO type.

When you're done chaining ZIO instances (containing either queries or whatever code you need), use TranzactIO's `Database` module to provide the `Connection` and execute the transaction.
`Database` can also provide a `Connection` in auto-commit mode, without a transaction.

TranzactIO comes with a very small amount of dependencies: only ZIO and ZIO-interop-Cats are required.

Any constructive criticism, bug report or offer to help is welcome. Just open an issue or a PR.



### Why ?

On my applications, I regularly have quite a bunch of business logic around my queries.
If I want to run that logic within a transaction, I have to wrap it with Doobie's `ConnectionIO`.
But I'm already using ZIO as my effect monad! I don't want another one...
In addition, IO monads in DB libraries (like Doobie's `ConnectionIO`) miss quite a bit of the operations that ZIO has.

That's where TranzactIO comes from. I wanted a way to use ZIO everywhere, and run the transaction whenever I decided to.





## Getting started


### Sbt setup

TranzactIO is available on the Sonatype Central Repository (see the Nexus badge on top of this README to get the version number). In your build.sbt:
```sbt
libraryDependencies += "io.github.gaelrenoux" %% "tranzactio" % TranzactIOVersion
```

In addition, you will need to declare the database access library you are using. For instance, with Doobie:
```sbt
libraryDependencies += "org.tpolecat" %% "doobie-core" % DoobieVersion
```



### Imports

Most of the time, you will need to import two packages.
The first is `io.github.gaelrenoux.tranzactio._` and contains Tranzactio's generic classes, like `DbException`.

The second one is specific to the DB-library you are using.
The names of most entities are the same for each DB-library: for instance, you'll always have the `tzio` function, or the `Connection` and `Database` classes.
The package is always named after the DB-library it is used with, e.g.:
- `io.github.gaelrenoux.tranzactio.doobie._`
- `io.github.gaelrenoux.tranzactio.anorm._`



### Wrapping a query

Just use `tzio` to wrap your usual query type!


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

Since Anorm doesn't provide an IO monad (or even a specific query type), `tzio` will provide the JDBC connection you need to run a query. The operation will be wrapped in a ZIO (as a blocking effect).

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

The `Database` module (from the same package as `tzio`) contains the methods needed to provide the `Connection` and run the transactions.

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

// If you have an additional environment, it would end up on the resulting effect as well.
val zioEnv: ZIO[Connection with Console, String, Long] = ???
val withEnv: ZIO[Database with Console, String, Long] = Database.transactionOrDie(zioEnv)

// Do you want to handle connection errors yourself? They will appear on the Left side of the Either.
val withSeparateErrors: ZIO[Database, Either[DbException, String], Long] = Database.transaction(zio)

// Are you only expecting errors coming from the DB ? Let's handle all of them at the same time.
val zioDbEx: ZIO[Connection, DbException, Long] = ???
val withDbEx: ZIO[Database, DbException, Long] = Database.transactionOrWiden(zioDbEx)

// Or maybe you're just grouping all errors together as exceptions.
val zioEx: ZIO[Connection, java.io.IOException, Long] = ???
val withEx: ZIO[Database, Exception, Long] = Database.transactionOrWiden(zioEx)

// You can also commit even on a failure (only rollbacking on a defect). Useful if you're using the failure channel for short-circuiting!
val commitOnFailure: ZIO[Database, String, Long] = Database.transactionOrDie(zio, commitOnFailure = true)

// And if you're actually not interested in a transaction, you can just auto-commit all queries.
val zioAutoCommit: ZIO[Database, String, Long] = Database.autoCommitOrDie(zio)
```



### Providing the Database

The `Database` methods return a ZIO instance which requires a `Database` as an environment.
This module is provided as usual through a `ZLayer`.

The most common way to construct a `Database` is using a `javax.sql.DataSource`, which your connection pool implementation (like HikariCP) should provide.
Alternatively (e.g. in a test environment), you can create a `DataSource` manually.

The layer to build a `Database` from a `javax.sql.DataSource` is on the `Database` object.
Here's an example for Doobie.
Again, the code for Anorm is identical, except it has a different import: `io.github.gaelrenoux.tranzactio.anorm._` instead of `io.github.gaelrenoux.tranzactio.doobie._`.

```scala
import io.github.gaelrenoux.tranzactio.doobie._
import javax.sql.DataSource
import zio._
import zio.clock.Clock

val dbLayer: ZLayer[Has[DataSource], Nothing, Database] = Database.fromDatasource
```



### More code samples
Find more in `src/main/samples`, or look below for some details.





## Detailed documentation

### Version compatibility

The table below indicates for each version of TranzactIO, the versions of ZIO or libraries it's been built with.
Check the backward compatibility information on those libraries to check which versions TranzactIO can support.

| TranzactIO | Scala       | ZIO          | Doobie       | Anorm        |
|------------|-------------|--------------|--------------|--------------|
| 0.1.0      |      2.13   | 1.0.0-RC17   | 0.8.6        | -            |
| 0.2.0      |      2.13   | 1.0.0-RC18-2 | 0.8.6        | -            |
| 0.3.0      |      2.13   | 1.0.0-RC18-2 | 0.8.6        | 2.6.5        |
| 0.4.0      |      2.13   | 1.0.0-RC19-2 | 0.9.0        | 2.6.5        |
| 0.5.0      |      2.13   | 1.0.0-RC20   | 0.9.0        | 2.6.5        |
| 0.6.0      |      2.13   | 1.0.0-RC21-1 | 0.9.0        | 2.6.5        |
| 1.0.0      |      2.13   | 1.0.0        | 0.9.0        | 2.6.7        |
| 1.0.1      |      2.13   | 1.0.0        | 0.9.0        | 2.6.7        |
| 1.1.0      |      2.13   | 1.0.3        | 0.9.2        | 2.6.7        |
| 1.2.0      |      2.13   | 1.0.3        | 0.9.2        | 2.6.7        |
| 1.3.0      |      2.13   | 1.0.5        | 0.9.4        | 2.6.10       |
| 2.0.0      |      2.13   | 1.0.5        | 0.12.1       | 2.6.10       |
| 2.1.0      | 2.12 2.13   | 1.0.9        | 0.13.4       | 2.6.10       |
| 3.0.0      | 2.12 2.13   | 1.0.11       | 1.0.0-RC2    | 2.6.10       |
| 4.0.0      | 2.12 2.13   | 2.0.0        | 1.0.0-RC2    | 2.6.10       |
| 4.1.0      | 2.12 2.13 3 | 2.0.2        | 1.0.0-RC2    | 2.7.0        |
| master     | 2.12 2.13 3 | 2.0.2        | 1.0.0-RC2    | 2.7.0        |



### Some definitions

#### Database operations

You will find reference through the documentation to ***Database operations***.
Those are the specific operations handled by Tranzactio, that are necessary to interact with a database:
- ***openConnection***
- ***setAutoCommit***
- ***commitConnection***
- ***rollbackConnection***
- ***closeConnection***

They correspond to specific methods in the `ConnectionSource` service.
You would not usually address that service directly, going through `Database` instead.
    
#### Error kinds

In TranzactIO, we recognize two kinds of errors relating to the DB: query errors, and connection errors:

**Query errors** happen when you run a specific query.
They can be timeouts, SQL syntax errors, constraint errors, etc.
When you have a `ZIO[Connection, E, A]`, E is the type for query errors.

**Connection errors** happen when you manage connections or transactions: opening connections, creating, commiting or rollbacking transactions, etc.
They are not linked to a specific query.
They are always reported as a `DbException`.



### Running a query (detailed version)

There are two families of methods on the `Database` class: `transaction` and `autoCommit`.
I'll only describe `transaction` here, keep in mind that there's an identical set of operations with `autoCommit` instead.

 When providing the `Connection` with `Database`, you have three variants of the `transaction` method, which will handle unrecovered connection errors differently.
- With `transaction`, the resulting error type is an `Either`: `Right` wraps a query error, and `Left` wraps a connection error. This is the most generic method, leaving you to handle all errors how you see fit.
- With `transactionOrDie`, connection errors are converted into defects, and do not appear in the type signature.
- With `transactionOrWiden`, the resulting error type will be the closest supertype of the query error type and `DbException`, and the error in the result may be a query error or a connection error. This is especially useful if your query error type is already `DbException` or directly `Exception`, as in the example below.

```scala
val zio: ZIO[Connection, E, A] = ???
val result1: ZIO[Database, Either[DbException, E], A] = Database.transaction(zio)
val result2: ZIO[Database, E, A] = Database.transactionOrDie(zio)
// assuming E extends Exception:
val result3: ZIO[Database, Exception, A] = Database.transactionOrWiden(zio)
```
 
A frequent case is to have an additional environment on your ZIO monad, e.g.: `ZIO[ZEnv with Connection, E, A]`.
All methods mentioned above will carry over the additional environment:
```scala
val zio: ZIO[ZEnv with Connection, E, A] = ???
val result1: ZIO[Database with ZEnv, Either[DbException, E], A] = Database.transaction(zio)
val result2: ZIO[Database with ZEnv, E, A] = Database.transactionOrDie(zio)
// assuming E extends Exception:
val result3: ZIO[Database with ZEnv, Exception, A] = Database.transactionOrWiden(zio)
```

All the `transaction` methods take an optional argument `commitOnFailure` (which defaults to `false`).
If `true`, the transaction will be committed on a failure (the `E` part in `ZIO[R, E, A]`), and will still be rollbacked on a defect.
Obviously, this argument does not exist on the `autoCommit` methods.

Finally, all those methods take an optional implicit argument of type `ErrorStrategies`. See **Handling connection errors** below for details.



### Handling connection errors (retries and timeouts)

TranzactIO provides no specific error handling for query errors.
Since you, as the developer, have direct access to the ZIO instance representing the query (or aggregation of queries), it's up to you to add timeouts or retries, recover from errors, etc.
However, you do not have access to the connection errors, which are hidden in the `ConnectionSource` and `Database` modules.

The error handling on connection errors is set up through an `ErrorStrategies` instance.
An `ErrorStrategies` is a group of `ErrorStrategy` instances, one for each of the database operations (`openConnection`, `setAutoCommit`, etc.)

#### Passing ErrorStrategies

TranzactIO looks for an `ErrorStrategies` in three different places, in order:
- You can pass an implicit `ErrorStrategies` parameter when calling the `Database` methods. If no implicit value is provided, it will defer to the next mechanism.
- When declaring the `Database` or `ConnectionSource` layer, you can pass an `ErrorStrategies` as a parameter.
- If no `ErrorStrategies` is defined either as an implicit parameter or in the layer definition, default is `ErrorStrategies.Nothing`: no retries and no timeouts.

```scala
implicit val es: ErrorStrategies = ErrorStrategies.retryForeverFixed(10.seconds)
Database.transaction(???) // es is passed implicitly to the method
```

```scala
val es: ErrorStrategies = ErrorStrategies.retryForeverFixed(10.seconds)
val dbLayerFromDatasource: ZLayer[Has[DataSource] with Clock, Nothing, Database] = Database.fromDatasource(es)
```

#### Defining an ErrorStrategies instance

To define an `ErrorStrategies`, start from the companion object, then add the retries and timeouts you want to apply.
Note that the operations are applied in the order you gave them (a timeout defined after a retry will apply a maximum duration to the retrying effect).
```scala
val es: ErrorStrategies = ErrorStrategies.timeout(3.seconds).retryCountExponential(10, 1.second, maxDelay = 10.seconds)
val es2: ErrorStrategies = ErrorStrategies.timeout(3.seconds).retryForeverFixed(1.second).timeout(1.minute)
```

If you want a specific strategy for some operation, you can set the singular `ErrorStrategy` manually:
```scala
val es: ErrorStrategies =
  ErrorStrategies.timeout(3.seconds).retryCountExponential(10, 1.second, maxDelay = 10.seconds)
    .copy(closeConnection = ErrorStrategy.retryForeverFixed(1.second)) // no timeout and fixed delay for closeConnection
```



#### Important caveat regarding timeouts

I strongly recommend that for timeouts, you use the mechanisms on your data source (or database) as you primary mechanism, and only use Tranzactio's timeouts as a backup if needed.

This is ***especially important*** for the `openConnection` operation: you should never have a timeout over this in TranzactIO, as it could lead to connection leaks!
For example, your app may encounter a timeout and abort the effect, but the data source is still going through and ends up providing a connection, which is lost.
Therefore, timeouts defined at the top-level of error strategies (as the first examples above) will ***not*** apply to `openConnection` (note that this only applies to timeouts, retries will indeed be applied to `openConnection`).

If after everything I said you find yourself wanting a headache, you can still define a timeout on `openConnection` by defining the corresponding `ErrorStrategy` manually.
```scala
// THIS IS A BAD IDEA, DON'T DO THIS.
val es: ErrorStrategies =
  ErrorStrategies.timeout(3.seconds).retryForeverFixed(1.second)
    .copy(openConnection = ErrorStrategy.timeout(3.seconds).retryForeverFixed(1.second))
```



### Single-connection Database

In some cases, you might want to have a `Database` module representing a single connection.
This might be useful for testing, or if you want to manually manage that connection.

For that purpose, you can use the layer `ConnectionSource.fromConnection`.
This layer requires a single JDBC `Connection`, and provides a `ConnectionSource` module.
You must then use the `Database.fromConnectionSource` layer to get the `Database` module.

Note that this ConnectionSource does not allow for concurrent usage, as that would lead to undeterministic results.
For example, some operation might close a transaction while a concurrent operation is between queries!
The non-concurrent behavior is enforced through a ZIO semaphore.



### Unit Testing

When unit testing, you typically use `ZIO.succeed` for your queries, instead of an actual SQL query.
However, the type signature still requires a Database, which you need to provide.
`Database.none` exists for this purpose: it satisfies the compiler, but does not provide a usable Database (so don't try to run any actual SQL queries against it).
```scala
import zio._
import doobie.implicits._
import io.github.gaelrenoux.tranzactio.DbException
import io.github.gaelrenoux.tranzactio.doobie._
import zio.clock.Clock

val liveQuery: ZIO[Connection, DbException, List[String]] = tzio { sql"SELECT name FROM users".query[String].to[List] }
val testQuery: ZIO[Connection, DbException, List[String]] = ZIO.succeed(List("Buffy Summers"))

val liveEffect: ZIO[Database, DbException, List[String]] = Database.transactionOrWiden(liveQuery)
val testEffect: ZIO[Database, DbException, List[String]] = Database.transactionOrWiden(testQuery)

val willFail: ZIO[Clock, Any, List[String]] = liveEffect.provideLayer(Database.none) // THIS WILL FAIL
val testing: ZIO[Clock, Any, List[String]] = testEffect.provideLayer(Database.none) // This will work
```





## FAQ

### I've got a compiler error: a type was inferred to be Any

Happens quite a lot when using ZIO, because Any is used to mark an 'empty' environment.
The best thing to do is to drop this warning from your configuration.
See https://github.com/zio/zio/pull/6455.



### When will tranzactio work with <insert DB library here>?

I want to add wrappers around more database access libraries.
Anorm was the second one I did, next should probably be Quill (based on the popularity of the project on GitHub),
but I'm completely unfamiliar with it.

Slick, however, is a problem. I know it quite well, tried to implement a TranzactIO module for it, and couldn't.
Transactions cannot be handled externally using Slick.
I don't think it's doable until this ticket is done: https://github.com/slick/slick/issues/1563
