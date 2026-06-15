package shipreq.webapp.server.db.migration

import cats.Applicative
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.typelevel.doobie._
import org.typelevel.doobie.implicits._
import org.typelevel.doobie.util.transactor.Strategy

private[migration] abstract class DbMigration extends BaseJavaMigration {

  override final def canExecuteInTransaction =
    true

  override final def migrate(context: Context): Unit = {

    val resourceXA: Resource[IO, Transactor[IO]] =
      Resource.pure {
        val conn = context.getConnection
        Transactor.fromConnection[IO](conn, None).copy(strategy0 = Strategy.void)
      }

    resourceXA
      .use(migration.transact(_))
      .unsafeRunSync()
  }

  protected def migration: ConnectionIO[_]

  protected final def point[A](a: => A): ConnectionIO[A] =
    Applicative[ConnectionIO].unit.map(_ => a)

  protected final def execute(sql: String) =
    Update0(sql, None).run
}
