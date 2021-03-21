package io.github.gaelrenoux.tranzactio

import zio.{Has, ZIO}

/** This is not a runnable test. It is here to check the type inference produces the expected types. */
trait DatabaseOpsCompileTest {

  import DatabaseOpsCompileTest._

  def z[R, E]: ZIO[R, E, Int] = ZIO.succeed(42)

  val serviceOperations: DatabaseOps.ServiceOps[Connection]

  object ServicesCheck {

    object TransactionR {
      val a: ZIO[Environment, Either[DbException, String], Int] =
        serviceOperations.transactionR(z[Connection with Environment, String])
      val b: ZIO[Environment, Either[DbException, String], Int] =
        serviceOperations.transactionR(z[Connection with Environment, String], commitOnFailure = true)
    }

    object Transaction {
      val a: ZIO[Any, Either[DbException, String], Int] =
        serviceOperations.transaction(z[Connection, String])
      val b: ZIO[Any, Either[DbException, String], Int] =
        serviceOperations.transaction(z[Connection, String], commitOnFailure = true)
    }

    object TransactionOrWidenR {
      val a: ZIO[Environment, Exception, Int] =
        serviceOperations.transactionOrWidenR(z[Connection with Environment, IllegalArgumentException])
      val b: ZIO[Environment, DbException, Int] =
        serviceOperations.transactionOrWidenR(z[Connection with Environment, DbException])
      val c: ZIO[Environment, Exception, Int] =
        serviceOperations.transactionOrWidenR(z[Connection with Environment, IllegalArgumentException], commitOnFailure = true)
      val d: ZIO[Environment, DbException, Int] =
        serviceOperations.transactionOrWidenR(z[Connection with Environment, DbException], commitOnFailure = true)
    }

    object TransactionOrWiden {
      val a: ZIO[Any, Exception, Int] =
        serviceOperations.transactionOrWiden(z[Connection, IllegalArgumentException])
      val b: ZIO[Any, DbException, Int] =
        serviceOperations.transactionOrWiden(z[Connection, DbException])
      val c: ZIO[Any, Exception, Int] =
        serviceOperations.transactionOrWiden(z[Connection, IllegalArgumentException], commitOnFailure = true)
      val d: ZIO[Any, DbException, Int] =
        serviceOperations.transactionOrWiden(z[Connection, DbException], commitOnFailure = true)
    }

    object TransactionOrDieR {
      val a: ZIO[Environment, String, Int] =
        serviceOperations.transactionOrDieR(z[Connection with Environment, String])
      val b: ZIO[Environment, String, Int] =
        serviceOperations.transactionOrDieR(z[Connection with Environment, String], commitOnFailure = true)
      val c: ZIO[Environment, String, Int] =
        serviceOperations.transactionOrDieR(z[Connection with Environment, String], commitOnFailure = true)
    }

    object TransactionOrDie {
      val a: ZIO[Any, String, Int] =
        serviceOperations.transactionOrDie(z[Connection, String])
      val b: ZIO[Any, String, Int] =
        serviceOperations.transactionOrDie(z[Connection, String], commitOnFailure = true)
    }

    object AutoCommitR {
      val a: ZIO[Environment, Either[DbException, String], Int] =
        serviceOperations.autoCommitR(z[Connection with Environment, String])
    }

    object AutoCommit {
      val a: ZIO[Any, Either[DbException, String], Int] =
        serviceOperations.autoCommit(z[Connection, String])
    }

    object AutoCommitOrWidenR {
      val a: ZIO[Environment, Exception, Int] =
        serviceOperations.autoCommitOrWidenR(z[Connection with Environment, IllegalArgumentException])
      val b: ZIO[Environment, DbException, Int] =
        serviceOperations.autoCommitOrWidenR(z[Connection with Environment, DbException])
    }

    object AutoCommitOrWiden {
      val a: ZIO[Any, Exception, Int] =
        serviceOperations.autoCommitOrWiden(z[Connection, IllegalArgumentException])
      val b: ZIO[Any, DbException, Int] =
        serviceOperations.autoCommitOrWiden(z[Connection, DbException])
    }

    object AutoCommitOrDieR {
      val a: ZIO[Environment, String, Int] =
        serviceOperations.autoCommitOrDieR(z[Connection with Environment, String])
    }

    object AutoCommitOrDie {
      val a: ZIO[Any, String, Int] =
        serviceOperations.autoCommitOrDie(z[Connection, String])
    }

  }


  val moduleOperations: DatabaseOps.ModuleOps[Connection, DatabaseService]

  object ModuleCheck {

    object TransactionR {
      val a: ZIO[Database with Environment, Either[DbException, String], Int] =
        moduleOperations.transactionR(z[Connection with Environment, String])
    }

    object Transaction {
      val a: ZIO[Database, Either[DbException, String], Int] =
        moduleOperations.transaction(z[Connection, String])
    }

    object TransactionOrWidenR {
      val a: ZIO[Database with Environment, Exception, Int] =
        moduleOperations.transactionOrWidenR(z[Connection with Environment, IllegalArgumentException])
      val b: ZIO[Database with Environment, DbException, Int] =
        moduleOperations.transactionOrWidenR(z[Connection with Environment, DbException])
    }

    object TransactionOrWiden {
      val a: ZIO[Database, Exception, Int] =
        moduleOperations.transactionOrWiden(z[Connection, IllegalArgumentException])
      val b: ZIO[Database, DbException, Int] =
        moduleOperations.transactionOrWiden(z[Connection, DbException])
    }

    object TransactionOrDieR {
      val a: ZIO[Database with Environment, String, Int] =
        moduleOperations.transactionOrDieR(z[Connection with Environment, String])
    }

    object TransactionOrDie {
      val a: ZIO[Database, String, Int] =
        moduleOperations.transactionOrDie[String, Int](z[Connection, String])
    }

    object AutoCommitR {
      val a: ZIO[Database with Environment, Either[DbException, String], Int] =
        moduleOperations.autoCommitR(z[Connection with Environment, String])
    }

    object AutoCommit {
      val a: ZIO[Database, Either[DbException, String], Int] =
        moduleOperations.autoCommit(z[Connection, String])
    }

    object AutoCommitOrWidenR {
      val a: ZIO[Database with Environment, Exception, Int] =
        moduleOperations.autoCommitOrWidenR(z[Connection with Environment, IllegalArgumentException])
      val b: ZIO[Database with Environment, DbException, Int] =
        moduleOperations.autoCommitOrWidenR(z[Connection with Environment, DbException])
    }

    object AutoCommitOrWiden {
      val a: ZIO[Database, Exception, Int] =
        moduleOperations.autoCommitOrWiden[Exception, Int](z[Connection, IllegalArgumentException])
      val b: ZIO[Database, DbException, Int] =
        moduleOperations.autoCommitOrWiden[DbException, Int](z[Connection, DbException])
    }

    object AutoCommitOrDieR {
      val a: ZIO[Database with Environment, String, Int] =
        moduleOperations.autoCommitOrDieR(z[Connection with Environment, String])
    }

    object AutoCommitOrDie {
      val a: ZIO[Database, String, Int] =
        moduleOperations.autoCommitOrDie[String, Int](z[Connection, String])
    }

  }

}

object DatabaseOpsCompileTest {

  trait Connection

  trait DatabaseService extends DatabaseOps.ServiceOps[Connection]

  type Database = Has[DatabaseService]

  type Environment = Has[String]
}
