package shipreq.webapp.base

import shipreq.base.util.Url
import shipreq.webapp.base.data.SecurityToken

object PublicUrls {
  val home           = Url.Relative("/")
  val register1      = Url.Relative("/register")
  val register2      = register1.thenParam[SecurityToken](_.value)
  val login          = Url.Relative("/login")
  val resetPassword1 = Url.Relative("/resetpw")
  val resetPassword2 = resetPassword1.thenParam[SecurityToken](_.value)
}
