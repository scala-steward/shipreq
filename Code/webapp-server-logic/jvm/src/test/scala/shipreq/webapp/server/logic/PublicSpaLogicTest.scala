package shipreq.webapp.server.logic

import scalaz.Name
import utest._
import shipreq.base.util._
import shipreq.taskman.api.Msg
import shipreq.webapp.base.test.WebappTestUtil._
import shipreq.webapp.base.user.{EmailAddr, UserId}
import shipreq.webapp.client.public.PublicSpaProtocols._

object PublicSpaLogicTest extends TestSuite {

  class Tester(allowRegister: Permission = Allow) extends MockInterpreters(_.copy(allowRegister = allowRegister)) {
    val logic = PublicSpaLogic[Name, Name]
    val initData = logic.initData.value

    def forwardTimeToEndOfConfirmationWindow(v: Validity): Unit =
      svr.forwardTimeToEndOfWindow(config.confirmationTokenLifespan, v)

    val ea = EmailAddr("blah@test.com")

    def runRegister1(i: Register.Fn1.Input): Register.Fn1.Response = svr.run(initData.register1)(i)
  }

  override def tests = TestSuite {

    'register1 {
      val t = new Tester(); import t._

      def runSuccessfully(tokensIssued: Int, msgsSubmitted: Int): Unit = {
        runRegister1(ea).needRight
        db.assertTokensIssued(tokensIssued)
        taskman.assertSubmitted(msgsSubmitted)
      }

      def assertRegistrationEmailSent() = {
        val m = taskman.assertLastSubmitted { case m: Msg.RegistrationRequested => m }
        assertEq(m.email.value, ea.value)
        assertContains(m.verifyEmailUrl, db.prevToken().value)
      }

      "email is valid and new -- should create a user, token and send email" - {
        runSuccessfully(1, 1)
        assertRegistrationEmailSent()
      }

      "pending, valid token exists -- should resend email" - {
        runSuccessfully(1, 1)
        forwardTimeToEndOfConfirmationWindow(Valid)
        runSuccessfully(1, 2)
        assertRegistrationEmailSent()
      }

      "pending, expired token exists -- should create a new token and email" - {
        runSuccessfully(1, 1)
        forwardTimeToEndOfConfirmationWindow(Invalid)
        runSuccessfully(2, 2)
        assertRegistrationEmailSent()
      }

      "email belongs to registered account -- should email with link to reset password" - {
        db.userPlaceholders = db.userPlaceholders + (ea -> DB.UserRegistration.Complete(UserId(7), svr.clock))
        runSuccessfully(0, 1)
        val m = taskman.assertLastSubmitted { case m: Msg.ReRegistrationAttempted => m }
        assertEq(m.email.value, ea.value)
      }

      "email is invalid -- should reject request" - {
        runRegister1(EmailAddr("not_an_email")).needLeft
        taskman.assertSubmitted(0)
      }

      'registrationsOff {
        val t = new Tester(Deny); import t._
        runRegister1(ea).needLeft
        ()
      }
    }

  }
}
