package shipreq.webapp.server.redis

import shipreq.webapp.base.data.ProjectId

final case class RedisSchema(prefix: String) {
  def snapshot(pid: ProjectId): RedisKey     = RedisKey(prefix + pid.value + ":ss")
  def events  (pid: ProjectId): RedisKey     = RedisKey(prefix + pid.value + ":es")
  def topic   (pid: ProjectId): RedisChannel = RedisChannel(prefix + pid.value + ":topic")
}

object RedisSchema {
  def default = RedisSchema("prj:")
}
