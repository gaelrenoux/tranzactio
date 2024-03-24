package io.github.gaelrenoux.tranzactio

import zio.ZIO
import zio.stream.ZStream

/** This is not a runnable test. It is here to check the type inference produces the expected types. */
trait DatabaseOpsCompileTest {

  import DatabaseOpsCompileTest._

  def z[R, E]: ZIO[R, E, Int] = ZIO.succeed(42)

  def zs[R, E]: ZStream[R, E, Int] = ZStream.succeed(42)

  val serviceOperations: DatabaseOps.ServiceOps[Connection]

  object ServicesCheck {

    object Transaction {
      val a: ZIO[Environment, Either[DbException, String], Int] =
        serviceOperations.transaction(z[Connection with Environment, String])
      val b: ZIO[Environment, Either[DbException, String], Int] =
        serviceOperations.transaction(z[Connection with Environment, String], commitOnFailure = true)

      val c: ZIO[Any, Either[DbException, String], Int] =
        serviceOperations.transaction(z[Connection, String])
      val d: ZIO[Any, Either[DbException, String], Int] =
        serviceOperations.transaction(z[Connection, String], commitOnFailure = true)
    }

    object TransactionOrWiden {
      val a: ZIO[Environment, Exception, Int] =
        serviceOperations.transactionOrWiden(z[Connection with Environment, IllegalArgumentException])
      val b: ZIO[Environment, DbException, Int] =
        serviceOperations.transactionOrWiden(z[Connection with Environment, DbException])
      val c: ZIO[Environment, Exception, Int] =
        serviceOperations.transactionOrWiden(z[Connection with Environment, IllegalArgumentException], commitOnFailure = true)
      val d: ZIO[Environment, DbException, Int] =
        serviceOperations.transactionOrWiden(z[Connection with Environment, DbException], commitOnFailure = true)

      val e: ZIO[Any, Exception, Int] =
        serviceOperations.transactionOrWiden(z[Connection, IllegalArgumentException])
      val f: ZIO[Any, DbException, Int] =
        serviceOperations.transactionOrWiden(z[Connection, DbException])
      val g: ZIO[Any, Exception, Int] =
        serviceOperations.transactionOrWiden(z[Connection, IllegalArgumentException], commitOnFailure = true)
      val h: ZIO[Any, DbException, Int] =
        serviceOperations.transactionOrWiden(z[Connection, DbException], commitOnFailure = true)
    }

    object TransactionOrDie {
      val a: ZIO[Environment, String, Int] =
        serviceOperations.transactionOrDie(z[Connection with Environment, String])
      val b: ZIO[Environment, String, Int] =
        serviceOperations.transactionOrDie(z[Connection with Environment, String], commitOnFailure = true)

      val c: ZIO[Any, String, Int] =
        serviceOperations.transactionOrDie(z[Connection, String])
      val d: ZIO[Any, String, Int] =
        serviceOperations.transactionOrDie(z[Connection, String], commitOnFailure = true)
    }

    object AutoCommit {
      val a: ZIO[Environment, Either[DbException, String], Int] =
        serviceOperations.autoCommit(z[Connection with Environment, String])

      val b: ZIO[Any, Either[DbException, String], Int] =
        serviceOperations.autoCommit(z[Connection, String])
    }

    object AutoCommitOrWiden {
      val a: ZIO[Environment, Exception, Int] =
        serviceOperations.autoCommitOrWiden(z[Connection with Environment, IllegalArgumentException])
      val b: ZIO[Environment, DbException, Int] =
        serviceOperations.autoCommitOrWiden(z[Connection with Environment, DbException])

      val c: ZIO[Any, Exception, Int] =
        serviceOperations.autoCommitOrWiden(z[Connection, IllegalArgumentException])
      val d: ZIO[Any, DbException, Int] =
        serviceOperations.autoCommitOrWiden(z[Connection, DbException])
    }

    object AutoCommitOrDie {
      val a: ZIO[Environment, String, Int] =
        serviceOperations.autoCommitOrDie(z[Connection with Environment, String])

      val b: ZIO[Any, String, Int] =
        serviceOperations.autoCommitOrDie(z[Connection, String])
    }

    object TransactionOrDieStream {
      val a: ZStream[Environment, String, Int] =
        serviceOperations.transactionOrDieStream(zs[Connection with Environment, String])
      val b: ZStream[Environment, String, Int] =
        serviceOperations.transactionOrDieStream(zs[Connection with Environment, String], commitOnFailure = true)

      val c: ZStream[Any, String, Int] =
        serviceOperations.transactionOrDieStream(zs[Connection, String])
      val d: ZStream[Any, String, Int] =
        serviceOperations.transactionOrDieStream(zs[Connection, String], commitOnFailure = true)
    }

    object AutoCommitStream {
      val a: ZStream[Environment, Either[DbException, String], Int] =
        serviceOperations.autoCommitStream(zs[Connection with Environment, String])

      val b: ZStream[Any, Either[DbException, String], Int] =
        serviceOperations.autoCommitStream(zs[Connection, String])
    }

    object AutoCommitOrWidenStream {
      val a: ZStream[Environment, Exception, Int] =
        serviceOperations.autoCommitOrWidenStream(zs[Connection with Environment, IllegalArgumentException])
      val b: ZStream[Environment, DbException, Int] =
        serviceOperations.autoCommitOrWidenStream(zs[Connection with Environment, DbException])

      val c: ZStream[Any, Exception, Int] =
        serviceOperations.autoCommitOrWidenStream(zs[Connection, IllegalArgumentException])
      val d: ZStream[Any, DbException, Int] =
        serviceOperations.autoCommitOrWidenStream(zs[Connection, DbException])
    }

    object AutoCommitOrDieStream {
      val a: ZStream[Environment, String, Int] =
        serviceOperations.autoCommitOrDieStream(zs[Connection with Environment, String])

      val b: ZStream[Any, String, Int] =
        serviceOperations.autoCommitOrDieStream(zs[Connection, String])
    }

  }


  val moduleOperations: DatabaseOps.ModuleOps[Connection, DatabaseService]

  object ModuleCheck {

    object Transaction {
      val a: ZIO[Database with Environment, Either[DbException, String], Int] =
        moduleOperations.transaction(z[Connection with Environment, String])

      val b: ZIO[Database, Either[DbException, String], Int] =
        moduleOperations.transaction(z[Connection, String])
    }

    object TransactionOrWiden {
      val a: ZIO[Database with Environment, Exception, Int] =
        moduleOperations.transactionOrWiden(z[Connection with Environment, IllegalArgumentException])
      val b: ZIO[Database with Environment, DbException, Int] =
        moduleOperations.transactionOrWiden(z[Connection with Environment, DbException])

      val c: ZIO[Database, Exception, Int] =
        moduleOperations.transactionOrWiden(z[Connection, IllegalArgumentException])
      val d: ZIO[Database, DbException, Int] =
        moduleOperations.transactionOrWiden(z[Connection, DbException])
    }

    object TransactionOrDie {
      val a: ZIO[Database with Environment, String, Int] =
        moduleOperations.transactionOrDie(z[Connection with Environment, String])

      val b: ZIO[Database, String, Int] =
        moduleOperations.transactionOrDie(z[Connection, String])
    }

    object AutoCommit {
      val a: ZIO[Database with Environment, Either[DbException, String], Int] =
        moduleOperations.autoCommit(z[Connection with Environment, String])

      val b: ZIO[Database, Either[DbException, String], Int] =
        moduleOperations.autoCommit(z[Connection, String])
    }

    object AutoCommitOrWiden {
      val a: ZIO[Database with Environment, Exception, Int] =
        moduleOperations.autoCommitOrWiden(z[Connection with Environment, IllegalArgumentException])
      val b: ZIO[Database with Environment, DbException, Int] =
        moduleOperations.autoCommitOrWiden(z[Connection with Environment, DbException])

      val c: ZIO[Database, Exception, Int] =
        moduleOperations.autoCommitOrWiden(z[Connection, IllegalArgumentException])
      val d: ZIO[Database, DbException, Int] =
        moduleOperations.autoCommitOrWiden(z[Connection, DbException])
    }

    object AutoCommitOrDie {
      val a: ZIO[Database with Environment, String, Int] =
        moduleOperations.autoCommitOrDie(z[Connection with Environment, String])

      val b: ZIO[Database, String, Int] =
        moduleOperations.autoCommitOrDie(z[Connection, String])
    }

    object TransactionOrDieStream {
      val a: ZStream[Database with Environment, String, Int] =
        moduleOperations.transactionOrDieStream(zs[Connection with Environment, String])

      val b: ZStream[Database, String, Int] =
        moduleOperations.transactionOrDieStream(zs[Connection, String])
    }

    object AutoCommitStream {
      val a: ZStream[Database with Environment, Either[DbException, String], Int] =
        moduleOperations.autoCommitStream(zs[Connection with Environment, String])

      val b: ZStream[Database, Either[DbException, String], Int] =
        moduleOperations.autoCommitStream(zs[Connection, String])
    }

    object AutoCommitOrWidenStream {
      val a: ZStream[Database with Environment, Exception, Int] =
        moduleOperations.autoCommitOrWidenStream(zs[Connection with Environment, IllegalArgumentException])
      val b: ZStream[Database with Environment, DbException, Int] =
        moduleOperations.autoCommitOrWidenStream(zs[Connection with Environment, DbException])

      val c: ZStream[Database, Exception, Int] =
        moduleOperations.autoCommitOrWidenStream(zs[Connection, IllegalArgumentException])
      val d: ZStream[Database, DbException, Int] =
        moduleOperations.autoCommitOrWidenStream(zs[Connection, DbException])
    }

    object AutoCommitOrDieStream {
      val a: ZStream[Database with Environment, String, Int] =
        moduleOperations.autoCommitOrDieStream(zs[Connection with Environment, String])

      val b: ZStream[Database, String, Int] =
        moduleOperations.autoCommitOrDieStream(zs[Connection, String])
    }

  }


  val typedServiceOperations: DatabaseTBase[Marker, Connection]
  val typedModuleOperations: DatabaseTBase.Module[Marker, Connection, DatabaseService]

  object DatabaseTChecks {

    val a: ZIO[Environment, Either[DbException, String], Int] =
      typedServiceOperations.transaction(z[Connection with Environment, String])

    val b: ZIO[Any, Either[DbException, String], Int] =
      typedServiceOperations.transaction(z[Connection, String])

    val c: ZIO[DatabaseT[Marker] with Environment, Either[DbException, String], Int] =
      typedModuleOperations.transaction(z[Connection with Environment, String])

    val d: ZIO[DatabaseT[Marker], Either[DbException, String], Int] =
      typedModuleOperations.transaction(z[Connection, String])

  }

}

object DatabaseOpsCompileTest {

  trait Connection

  trait DatabaseService extends DatabaseOps.ServiceOps[Connection]

  type Database = DatabaseService

  type DatabaseT[M] = DatabaseTBase[M, Connection]

  trait Environment

  trait Marker
}
