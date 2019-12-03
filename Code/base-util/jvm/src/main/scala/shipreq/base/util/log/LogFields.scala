package shipreq.base.util.log

/** Additional fields to add to log messages.
  *
  * The whole point of this is safety in ElasticSearch wrt to its stupid auto-schema functionality.
  *
  * Rule 1: Never delete anything from the list below.
  * Rule 2: Never change the type of anything below.
  */
object LogFields {

  object http {

    object request {
      val body   = LogField.Text("shipreq.http.request.body")
      val method = LogField.Text("shipreq.http.request.method")
      val url    = LogField.Text("shipreq.http.request.url")
    }

    object response {
      val body  = LogField.Text           ("shipreq.http.response.body")
      val code  = LogField.Long           ("shipreq.http.response.code")
      val durMs = LogField.Long.durationMs("shipreq.http.response.duration.ms")
    }
  }

}
