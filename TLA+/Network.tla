---------------------------------------------------- MODULE Network ----------------------------------------------------

EXTENDS Naturals, Sequences, Util

VARIABLE queue

Invariants(Msg) ==
  queue \in Seq(Msg)

Init ==
  queue = <<>>

------------------------------------------------------------------------------------------------------------------------

IsEmpty ==
  queue = <<>>

\* Input:
\*   select      :: Msg        => Boolean -- whether or not a Msg is one you want to find
\*   sameChannel :: (Msg, Msg) => Boolean -- whether two messages are on the same channel (i.e. same to/from)
\*                                           only the first msg per channel is eligible (i.e. order matters)
\* Output:
\*   0 if not found
\*   otherwise msg index
FindNextMsgPerChannel(select(_), sameChannel(_, _)) ==
  LET isNotNext(i) ==
        LET n == queue[i] IN
          \E j \in 1..(i-1) :
            LET m == queue[j] IN
              sameChannel(n, m)
      i == SeqIndexOf(queue, select)
  IN IF i = 0 | (i != 1 & isNotNext(i)) THEN
       0
     ELSE
       i

\* Convenience method for FindNextMsgPerChannel where
\*   select      = _.type = type
\*   sameChannel = (n,m) => (n.from = m.from) && (n.to = m.to)
FindNextMsgByType(type) ==
  FindNextMsgPerChannel(
    LAMBDA m  : m.type = type,
    LAMBDA n,m: (n.from = m.from) & (n.to = m.to))

ModRecv       (q, i)       == RemoveAt(q, i)
ModSend       (q, msg)     == Append(q, msg)
ModSendSet    (q, msgs)    == SetFold(msgs, q, Append)
ModRecvSend   (q, i, msg)  == ModSend(ModRecv(q, i), msg)
ModRecvSendSeq(q, i, msgs) == ModRecv(q, i) \o msgs
ModRecvSendSet(q, i, msgs) == ModSendSet(ModRecv(q, i), msgs)

Recv       (i)       == queue' = ModRecv       (queue, i)
Send       (msg)     == queue' = ModSend       (queue, msg)
SendSet    (msgs)    == queue' = ModSendSet    (queue, msgs)
RecvSend   (i, msg)  == queue' = ModRecvSend   (queue, i, msg)
RecvSendSeq(i, msgs) == queue' = ModRecvSendSeq(queue, i, msgs)
RecvSendSet(i, msgs) == queue' = ModRecvSendSet(queue, i, msgs)

========================================================================================================================
