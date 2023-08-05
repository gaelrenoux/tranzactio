package io.github.gaelrenoux.tranzactio

/** To be used by libraries where a DbContext is not necessary */
object EmptyDbContext {
  implicit val Default: EmptyDbContext.type = EmptyDbContext
}
