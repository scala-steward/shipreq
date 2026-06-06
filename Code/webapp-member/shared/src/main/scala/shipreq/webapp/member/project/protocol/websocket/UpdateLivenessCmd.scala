package shipreq.webapp.member.project.protocol.websocket

import shipreq.webapp.member.project.text.Text.DeletionReason

sealed trait UpdateLivenessCmd

object UpdateLivenessCmd {

  final case class Delete(reason: DeletionReason.OptionalText) extends UpdateLivenessCmd

  case object Restore extends UpdateLivenessCmd

  object CodecsV1 {
    import boopickle.DefaultBasic._
    import shipreq.webapp.member.project.protocol.binary.v1.Rev6.AtomPicklers.instances._

    implicit lazy val picklerUpdateLivenessCmd: Pickler[UpdateLivenessCmd] =
      new Pickler[UpdateLivenessCmd] {
        private[this] final val KeyDelete  = 'd'
        private[this] final val KeyRestore = 'r'
        override def pickle(a: UpdateLivenessCmd)(implicit state: PickleState): Unit =
          a match {
            case b: UpdateLivenessCmd.Delete => state.enc.writeByte(KeyDelete ); state.pickle(b)
            case UpdateLivenessCmd.Restore   => state.enc.writeByte(KeyRestore)
          }
        override def unpickle(implicit state: UnpickleState): UpdateLivenessCmd =
          state.dec.readByte match {
            case KeyDelete  => state.unpickle[UpdateLivenessCmd.Delete]
            case KeyRestore => UpdateLivenessCmd.Restore
          }
      }

    implicit lazy val picklerUpdateLivenessCmdDelete: Pickler[UpdateLivenessCmd.Delete] =
      transformPickler(UpdateLivenessCmd.Delete.apply)(_.reason)
  }
}
