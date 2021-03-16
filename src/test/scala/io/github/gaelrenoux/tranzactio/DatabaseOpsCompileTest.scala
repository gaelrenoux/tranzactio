package io.github.gaelrenoux.tranzactio

import zio.{Has, ZIO}

/** This is not a runnable test. It is here to check the type inference produces the expected types. */
trait DatabaseOpsCompileTest {

  import DatabaseOpsCompileTest._

  def z[R, E]: ZIO[R, E, Int] = ZIO.succeed(42)

  val serviceOperations: DatabaseOps.ServiceOps[Connection]

  val serviceChecks: Unit = {

    /* transactionR */
    val _: ZIO[Environment, Either[DbException, String], Int] =
      serviceOperations.transactionR[Environment](z[Connection with Environment, String])
    val _: ZIO[Environment, Either[DbException, String], Int] =
      serviceOperations.transactionR[Environment](z[Connection with Environment, String], commitOnFailure = true)

    /* transaction */
    val _: ZIO[Any, Either[DbException, String], Int] =
      serviceOperations.transaction(z[Connection, String])
    val _: ZIO[Any, Either[DbException, String], Int] =
      serviceOperations.transaction(z[Connection, String], commitOnFailure = true)

    /* transactionOrWidenR */
    val _: ZIO[Environment, Exception, Int] =
      serviceOperations.transactionOrWidenR[Environment](z[Connection with Environment, IllegalArgumentException])
    val _: ZIO[Environment, DbException, Int] =
      serviceOperations.transactionOrWidenR[Environment](z[Connection with Environment, DbException])
    val _: ZIO[Environment, Exception, Int] =
      serviceOperations.transactionOrWidenR[Environment](z[Connection with Environment, IllegalArgumentException], commitOnFailure = true)
    val _: ZIO[Environment, DbException, Int] =
      serviceOperations.transactionOrWidenR[Environment](z[Connection with Environment, DbException], commitOnFailure = true)

    /* transactionOrWiden */
    val _: ZIO[Any, Exception, Int] =
      serviceOperations.transactionOrWiden(z[Connection, IllegalArgumentException])
    val _: ZIO[Any, DbException, Int] =
      serviceOperations.transactionOrWiden(z[Connection, DbException])
    val _: ZIO[Any, Exception, Int] =
      serviceOperations.transactionOrWiden(z[Connection, IllegalArgumentException], commitOnFailure = true)
    val _: ZIO[Any, DbException, Int] =
      serviceOperations.transactionOrWiden(z[Connection, DbException], commitOnFailure = true)

    /* transactionOrDieR (specifying remainder) */
    val _: ZIO[Environment, String, Int] =
      serviceOperations.transactionOrDieR[Environment](z[Connection with Environment, String])
    val _: ZIO[Environment, String, Int] =
      serviceOperations.transactionOrDieR[Environment](z[Connection with Environment, String], commitOnFailure = true)
    val _: ZIO[Environment, String, Int] =
      serviceOperations.transactionOrDieR[Environment](z[Connection with Environment, String], commitOnFailure = true)

    /* transactionOrDieR (without specifying remainder) */
    val _: ZIO[Environment, String, Int] =
      serviceOperations.transactionOrDieR(z[Connection with Environment, String])
    val _: ZIO[Environment, String, Int] =
      serviceOperations.transactionOrDieR(z[Connection with Environment, String], commitOnFailure = true)
    val _: ZIO[Environment, String, Int] =
      serviceOperations.transactionOrDieR(z[Connection with Environment, String], commitOnFailure = true)

    /* transactionOrDie */
    val _: ZIO[Any, String, Int] =
      serviceOperations.transactionOrDie(z[Connection, String])
    val _: ZIO[Any, String, Int] =
      serviceOperations.transactionOrDie(z[Connection, String], commitOnFailure = true)

    /* autoCommitR */
    val _: ZIO[Environment, Either[DbException, String], Int] =
      serviceOperations.autoCommitR[Environment](z[Connection with Environment, String])

    /* autoCommit */
    val _: ZIO[Any, Either[DbException, String], Int] =
      serviceOperations.autoCommit(z[Connection, String])

    /* autoCommitOrWidenR (specifying remainder)*/
    val _: ZIO[Environment, Exception, Int] =
      serviceOperations.autoCommitOrWidenR[Environment](z[Connection with Environment, IllegalArgumentException])
    val _: ZIO[Environment, DbException, Int] =
      serviceOperations.autoCommitOrWidenR[Environment](z[Connection with Environment, DbException])

    /* autoCommitOrWidenR (without specifying remainder)*/
    val _: ZIO[Environment, Exception, Int] =
      serviceOperations.autoCommitOrWidenR(z[Connection with Environment, IllegalArgumentException])
    val c: ZIO[Environment, DbException, Int] =
      serviceOperations.autoCommitOrWidenR(z[Connection with Environment, DbException])

    /* autoCommitOrWiden */
    val _: ZIO[Any, Exception, Int] =
      serviceOperations.autoCommitOrWiden(z[Connection, IllegalArgumentException])
    val _: ZIO[Any, DbException, Int] =
      serviceOperations.autoCommitOrWiden(z[Connection, DbException])

    /* autoCommitOrDieR */
    val _: ZIO[Environment, String, Int] =
      serviceOperations.autoCommitOrDieR[Environment](z[Connection with Environment, String])

    /* autoCommitOrDie */
    val _: ZIO[Any, String, Int] =
      serviceOperations.autoCommitOrDie(z[Connection, String])
  }


  val moduleOperations: DatabaseOps.ModuleOps[Connection, DatabaseService]

  val moduleChecks: Unit = {

    /* transaction */
    val _: ZIO[Database with Environment, Either[DbException, String], Int] =
      moduleOperations.transactionR[Environment](z[Connection with Environment, String])
    val _: ZIO[Database, Either[DbException, String], Int] =
      moduleOperations.transaction(z[Connection, String])

    val _: ZIO[Database with Environment, Exception, Int] =
      moduleOperations.transactionOrWidenR[Environment](z[Connection with Environment, IllegalArgumentException])
    val _: ZIO[Database with Environment, DbException, Int] =
      moduleOperations.transactionOrWidenR[Environment](z[Connection with Environment, DbException])
    val _: ZIO[Database, Exception, Int] =
      moduleOperations.transactionOrWiden(z[Connection, IllegalArgumentException])
    val _: ZIO[Database, DbException, Int] =
      moduleOperations.transactionOrWiden(z[Connection, DbException])

    val _: ZIO[Database with Environment, String, Int] =
      moduleOperations.transactionOrDieR(z[Connection with Environment, String])
    val _: ZIO[Database, String, Int] =
      moduleOperations.transactionOrDie[String, Int](z[Connection, String])

    /* auto-commit */
    val _: ZIO[Database with Environment, Either[DbException, String], Int] =
      moduleOperations.autoCommitR[Environment](z[Connection with Environment, String])
    val _: ZIO[Database, Either[DbException, String], Int] =
      moduleOperations.autoCommit(z[Connection, String])

    val _: ZIO[Database with Environment, Exception, Int] =
      moduleOperations.autoCommitOrWidenR[Environment](z[Connection with Environment, IllegalArgumentException])
    val _: ZIO[Database with Environment, DbException, Int] =
      moduleOperations.autoCommitOrWidenR[Environment](z[Connection with Environment, DbException])
    val _: ZIO[Database, Exception, Int] =
      moduleOperations.autoCommitOrWiden[Exception, Int](z[Connection, IllegalArgumentException])
    val _: ZIO[Database, DbException, Int] =
      moduleOperations.autoCommitOrWiden[DbException, Int](z[Connection, DbException])

    val _: ZIO[Database with Environment, String, Int] =
      moduleOperations.autoCommitOrDieR[Environment](z[Connection with Environment, String])
    val _: ZIO[Database, String, Int] =
      moduleOperations.autoCommitOrDie[String, Int](z[Connection, String])
  }

}

object DatabaseOpsCompileTest {

  trait Connection

  trait DatabaseService extends DatabaseOps.ServiceOps[Connection]

  type Database = Has[DatabaseService]

  type Environment = Has[String]
}
