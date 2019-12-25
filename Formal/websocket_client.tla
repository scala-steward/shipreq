----------------------------------------------- MODULE websocket_client -----------------------------------------------

EXTENDS TLC

VARIABLES retry,     \* Whether more retries are allowed
          scheduled, \* Whether a new connection has been scheduled
          ws         \* The current websocket state

vars == << retry, scheduled, ws >>

None       == "None"       \* None: Option[Instance]
Connecting == "Connecting" \* Some(ws) if ws.readyState = Connecting
Open       == "Open"       \* Some(ws) if ws.readyState = Open
Closing    == "Closing"    \* Some(ws) if ws.readyState = Closing
Closed     == "Closed"     \* Some(ws) if ws.readyState = Closed

TypeInvariants ==
  /\ retry     \in BOOLEAN
  /\ scheduled \in BOOLEAN
  /\ ws        \in {None, Connecting, Open, Closing, Closed}
\*  /\ PrintT([retry |-> retry, scheduled |-> scheduled, ws |-> ws])

DataInvariants ==
  /\ scheduled => ws \in {None, Closed}

Init ==
  /\ retry \in BOOLEAN
  /\ scheduled = FALSE
  /\ ws \in {None, Connecting}

------------------------------------------------------------------------------------------------------------------------

scheduleReconnect(assertNotScheduled) ==
  IF retry
  THEN /\ assertNotScheduled => Assert(~scheduled, "Zombie scheduled task detected.")
       /\ scheduled' = TRUE
       /\ retry' \in BOOLEAN \* move on to next retry
  ELSE /\ scheduled' = FALSE
       /\ UNCHANGED retry

------------------------------------------------------------------------------------------------------------------------

WS_Open ==
  /\ ws = Connecting
  /\ ws' = Open
  /\ retry' \in BOOLEAN \* reset retry status (counter)
  /\ UNCHANGED << scheduled >>

WS_Closing ==
  /\ ws \in {Connecting, Open}
  /\ ws' = Closing
  /\ UNCHANGED << retry, scheduled >>

WS_Closed ==
  /\ ws \in {Connecting, Open, Closing}
  /\ ws' = Closed
  /\ scheduleReconnect(TRUE)

ScheduledTaskExecutes ==
  /\ scheduled = TRUE
  /\ \/ \* Connection succeeds
        /\ scheduled' = FALSE
        /\ ws' = Connecting
        /\ UNCHANGED << retry >>
     \/ \* Connection fails
        /\ ws' = None
        /\ scheduleReconnect(FALSE)

ConnectNow ==
  /\ ws \in {None,Closed}
  \* clearTimer here
  /\ \/ \* Connection succeeds
        /\ ws' = Connecting
        /\ scheduled' = FALSE
        /\ retry' \in BOOLEAN \* reset retry status (counter)
     \/ \* Connection fails
        /\ ws' = None
        /\ scheduleReconnect(FALSE) \* because clearTimer above

Close ==
  /\ retry' = FALSE
  /\ IF ws = Open
     THEN ws' = Closing
     ELSE UNCHANGED ws
  /\ UNCHANGED scheduled

Next ==
  \/ WS_Open
  \/ WS_Closing
  \/ WS_Closed
  \/ ScheduledTaskExecutes
  \/ ConnectNow
  \/ Close

------------------------------------------------------------------------------------------------------------------------

Liveness ==
  /\ WF_vars(WS_Open)
  /\ WF_vars(ScheduledTaskExecutes)
  /\ WF_vars(ConnectNow)

Spec == Init /\ [][Next]_vars /\ Liveness

========================================================================================================================
