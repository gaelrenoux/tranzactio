package gaelrenoux.tranzactio

/** Wraps all exceptions that may happen when working with the DB. */
case class DbException(cause: Throwable) extends Exception(cause)
