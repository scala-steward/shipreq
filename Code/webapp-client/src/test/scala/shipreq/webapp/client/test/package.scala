package shipreq.webapp.client

package object test {

  @inline final def $ = Sizzle

  object PrepareEnv {
    def apply(): Unit = ()
    shipreq.webapp.client.app.ui.Style
  }
}
