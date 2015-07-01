package shipreq.webapp.base.hash

object HashScheme {
  val default = new DataHash(MurmurHash3)
}
