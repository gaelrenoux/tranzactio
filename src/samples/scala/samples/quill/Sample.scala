package samples.quill

import samples.Person

object Sample {

  import io.getquill._

  val ctx = new SqlMirrorContext(MirrorSqlDialect, Literal)

  //import ctx._

  val persons: ctx.Quoted[ctx.EntityQuery[Person]] = ctx.quote {
    ctx.query[Person]
  }

  val personsIo: ctx.IO[ctx.QueryMirror[Person], ctx.Effect.Read] = ctx.runIO(persons)

}
