package gaelrenoux.tranzactio

/** One common way to represent problems when connectiong to the DB are exceptions. This wraps them. */
case class DbException(cause: Throwable) extends Exception(cause)
