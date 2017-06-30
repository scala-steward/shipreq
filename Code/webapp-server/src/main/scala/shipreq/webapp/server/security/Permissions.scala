package shipreq.webapp.server.security

import Permission.Ctx

object Permissions {

  val accessProject: Permission = new TypicalPermission {
    override def name = "accessProject"
    override def warnOnFailure = true
    override def check(ctx: Ctx) =
      for {
        u <- ctx.user
        p <- ctx.project
      } yield
        cmp(p.owner, u.id, "project.owner == user.id")
  }

  val admin = new TypicalPermission {
    def warnOnFailure: Boolean = true
    def name: String = "admin"
    def check(ctx: Ctx): Option[Boolean] =
      ctx.user.map(_ hasRole Roles.Admin.name)
  }
}
