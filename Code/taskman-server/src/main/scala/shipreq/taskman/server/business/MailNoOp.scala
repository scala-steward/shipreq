package shipreq.taskman.server.business

import scala.runtime.AbstractFunction1
import shipreq.base.util.FxModule._
import shipreq.base.util.log.HasLogger
import shipreq.taskman.server.logic.business.BusinessOp.SendEmail

/** An interpretter for [[SendEmail]] that simply logs and does nothing. */
object MailNoOp extends AbstractFunction1[SendEmail, Fx[Unit]] with HasLogger {

  override def apply(op: SendEmail): Fx[Unit] =
    Fx {
      logger.info(s"""Ignoring SendMail
${op.envelope}

${op.content.subject}

${op.content.body}""")
    }
}
