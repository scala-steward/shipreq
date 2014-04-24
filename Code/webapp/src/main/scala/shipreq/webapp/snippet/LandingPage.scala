package shipreq.webapp.snippet

import scalaz.{Failure, Success}
import net.liftweb.http.SHtml
import net.liftweb.http.js.{JsCmds, JsCmd}
import net.liftweb.util.CssSel
import net.liftweb.util.Helpers._
import shipreq.webapp.util.HtmlTransformExt.ajaxSubmitOnClick
import shipreq.webapp.feature.validation.{ValidationResult, Validators}
import shipreq.webapp.lib.SnippetHelpers
import shipreq.taskman.api.Msg.LandingPageHit

object LandingPage extends SnippetHelpers {

  private val firstNameExtractor = "\\s.+$".r
  private val jsDisableForm = JsCmds.Run("$('#form').find('input,textarea,button').prop('disabled',true)")

  def form: CssSel = {
    var nameI      : String  = ""
    var emailI     : String  = ""
    var msgI       : String  = ""
    var newsletterI: Boolean = true

    def nameV       = Validators.landingPage.name.correctAndValidate(nameI)
    def emailV      = Validators.landingPage.email.correctAndValidateEA(emailI)
    def msgV        = Validators.landingPage.msg.correctAndValidate(msgI)
    def newsletterV = ValidationResult(newsletterI)

    def onSubmit: JsCmd =
      Validators.Ap.apply4(emailV, nameV, msgV, newsletterV)(LandingPageHit) match {
        case Failure(f) =>
          JsCmds.Alert(f.toText)
        case Success(msg) =>
          taskman1(_ submitMsg msg)
          val firstName = firstNameExtractor.replaceFirstIn(msg.name, "")
          jsDisableForm & JsCmds.Alert(s"Thank you, $firstName.\n\nWe'll be in touch!")
      }

    ".n" #> SHtml.onSubmit(nameI = _) &
    ".e" #> SHtml.onSubmit(emailI = _) &
    ".m" #> SHtml.onSubmit(msgI = _) &
    ".newsletter input" #> SHtml.onSubmitBoolean(newsletterI = _) &
    ":submit" #> ajaxSubmitOnClick(onSubmit _)
  }
}
