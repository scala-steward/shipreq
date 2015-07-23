package shipreq.webapp.base.hash

object HashScheme {

  val latest: HashScheme = new DataHash(MurmurHash3)
}
