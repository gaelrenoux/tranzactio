package io.github.gaelrenoux.tranzactio.test

import java.sql.{Array => _, _}
import java.util.Properties
import java.util.concurrent.Executor
import java.{sql, util}

/** Java SQL connection implementation, where all methods fail with UnsupportedOperationException. */
// scalastyle:off number.of.methods
object NoopJdbcConnection extends Connection {
  override def createStatement(): Statement = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def prepareStatement(s: String): PreparedStatement = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def prepareCall(s: String): CallableStatement = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def nativeSQL(s: String): String = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def setAutoCommit(b: Boolean): Unit = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def getAutoCommit: Boolean = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def commit(): Unit = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def rollback(): Unit = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def close(): Unit = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def isClosed: Boolean = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def getMetaData: DatabaseMetaData = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def setReadOnly(b: Boolean): Unit = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def isReadOnly: Boolean = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def setCatalog(s: String): Unit = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def getCatalog: String = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def setTransactionIsolation(i: Int): Unit = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def getTransactionIsolation: Int = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def getWarnings: SQLWarning = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def clearWarnings(): Unit = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def createStatement(i: Int, i1: Int): Statement = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def prepareStatement(s: String, i: Int, i1: Int): PreparedStatement = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def prepareCall(s: String, i: Int, i1: Int): CallableStatement = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def getTypeMap: util.Map[String, Class[_]] = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def setTypeMap(map: util.Map[String, Class[_]]): Unit = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def setHoldability(i: Int): Unit = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def getHoldability: Int = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def setSavepoint(): Savepoint = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def setSavepoint(s: String): Savepoint = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def rollback(savepoint: Savepoint): Unit = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def releaseSavepoint(savepoint: Savepoint): Unit = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def createStatement(i: Int, i1: Int, i2: Int): Statement = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def prepareStatement(s: String, i: Int, i1: Int, i2: Int): PreparedStatement = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def prepareCall(s: String, i: Int, i1: Int, i2: Int): CallableStatement = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def prepareStatement(s: String, i: Int): PreparedStatement = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def prepareStatement(s: String, ints: Array[Int]): PreparedStatement = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def prepareStatement(s: String, strings: Array[String]): PreparedStatement = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def createClob(): Clob = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def createBlob(): Blob = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def createNClob(): NClob = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def createSQLXML(): SQLXML = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def isValid(i: Int): Boolean = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def setClientInfo(s: String, s1: String): Unit = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def setClientInfo(properties: Properties): Unit = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def getClientInfo(s: String): String = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def getClientInfo: Properties = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def createArrayOf(s: String, objects: Array[AnyRef]): sql.Array = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def createStruct(s: String, objects: Array[AnyRef]): Struct = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def setSchema(s: String): Unit = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def getSchema: String = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def abort(executor: Executor): Unit = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def setNetworkTimeout(executor: Executor, i: Int): Unit = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def getNetworkTimeout: Int = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def unwrap[T](aClass: Class[T]): T = throw new UnsupportedOperationException("NoopJdbcConnection")

  override def isWrapperFor(aClass: Class[_]): Boolean = throw new UnsupportedOperationException("NoopJdbcConnection")
}
