------------------------------------------------- MODULE project_cache -------------------------------------------------

EXTENDS FiniteSets,
        Naturals,
        TLC

CONSTANT User,
         Request,
         IncludeUserDisconnect

ASSUME /\ IsFiniteSet(User)
       /\ IsFiniteSet(Request)
       /\ IncludeUserDisconnect \in BOOLEAN

VARIABLES db,       \* The state of the DB
          redis,    \* The state of Redis
          procsI,
          procs,    \* The state of request processors (i.e. threads in webapps)
          pub,      \* Set of events being published
          userState \* Users' states

vars == << db, redis, procsI, procs, pub, userState >>

varDesc == [db        |-> db.ver,
            redis     |-> <<redis.ver, redis.events>>,
            procsI    |-> procsI,
            procs     |-> procs,
            pub       |-> pub,
            userState |-> userState]

------------------------------------------------------------------------------------------------------------------------

Min[as \in SUBSET Nat] == CHOOSE a \in as : \A b \in as : a <= b
Max[as \in SUBSET Nat] == CHOOSE a \in as : \A b \in as : a >= b

\* Remove(set, el) == {a \in set : a /= el}
Remove(set, el) == set \ {el}

Replace(set, old, new) == { IF a = old THEN new ELSE a : a \in set}

ApplyEvents[v \in Nat, es \in SUBSET Nat] ==
  LET n == v + 1
  IN IF n \in es
     THEN ApplyEvents[n, es \ {n}]
     ELSE <<v,es>>


RedisPartialVer == IF redis.events = {} THEN redis.ver ELSE Max[redis.events]
RedisTotalVer   == IF redis.ver    = 0  THEN 0         ELSE RedisPartialVer

OnlineUsers == {u \in User : userState[u].status /= "offline"}

------------------------------------------------------------------------------------------------------------------------

TypeInvariants ==
  /\ db \in [ver: Nat] \* The version of the Project aka the number of events
  /\ redis \in [
       ver   : Nat,        \* The version of a Project snapshot, or 0 if cache empty
       events: SUBSET Nat] \* A set of events represented by their version numbers
  /\ pub \in SUBSET (User \X Nat) \* Set of (target user, event)
  /\ procsI \in SUBSET [
       status  : {"ReadRedis", "ReadDB", "WriteRedis", "Respond"},
       user    : User,
       ver     : Nat] \* The version of the Project in memory (0=none)
  /\ procs \in SUBSET [
       req     : Request,
       status  : {"ReadRedis", "ReadDB", "WriteRedis1", "WriteDB", "WriteRedis2", "Done"},
       user    : User,
       redisVer: Nat, \* The version of Redis at the last read from Redis
       ver     : Nat] \* The version of the Project in memory (0=none)
  /\ userState \in [
       User -> [
         status : {"offline", "loading", "active"},
         ver    : Nat,              \* The version of the built Project
         future : SUBSET Nat,       \* Future events that can't be applied cos intermediate event is missing
         reqs   : SUBSET Request ]] \* Requests for which a response hasn't be received

DataInvariants ==
  \* /\ PrintT(varDesc)
  /\ \A u \in User :
    LET s == userState[u]
    IN /\ s.status = "active" => s.ver > 0
       /\ s.status /= "active" => s.reqs = {}
       /\ s.ver <= db.ver
       /\ \A e \in s.future : e > s.ver + 1
  /\ redis.ver <= db.ver
  /\ \A e \in redis.events :
    /\ \* No gaps in Redis events
       IF redis.ver > 0
       THEN (e - 1) \in (redis.events \union {redis.ver})
       ELSE (e - 1) \in (redis.events) \/ e = Min[redis.events]

OfflineUser == [
  status |-> "offline",
  ver    |-> 0,
  future |-> {},
  reqs   |-> {}]

Init ==
  /\ db        = [ver |-> 1]
  /\ redis     = [ver |-> 0, events |-> {}]
  /\ procsI    = {}
  /\ procs     = {}
  /\ pub       = {}
  /\ userState = [u \in User |-> OfflineUser]

------------------------------------------------------------------------------------------------------------------------

RedisWriteSnapshot(ver, OnOk, OnFail) ==
  IF ver > RedisTotalVer
  THEN
    /\ redis' = [ver    |-> ver,
                 events |-> IF ver + 1 \in redis.events THEN {e \in redis.events : e > ver} ELSE {}]
    /\ OnOk
  ELSE \* Redis has a more recent state than this proc
    /\ UNCHANGED redis
    /\ OnFail

------------------------------------------------------------------------------------------------------------------------

\* In reality this is: open page, establish websocket, receive project, subscribe to pub/sub channel
UserConnect == \E u \in User :
  /\ userState[u].status = "offline"
  /\ \A p \in procs : p.user /= u \* A new user (connection) is distinct.
                                  \* If the model value is still being used in an orphan proc, it can be recycled here yet
  /\ userState' = [userState EXCEPT ![u].status = "loading"]
  /\ procsI' = procsI \union {[user |-> u, status |-> "ReadRedis", ver |-> 0]}
  /\ UNCHANGED << db, redis, procs, pub >>

\* TODO Also model the fact that i'll start preloading on HTTP GET before the websocket connects

Load_ReadRedis == \E p \in procsI :
  /\ p.status = "ReadRedis"
  /\ procsI' = Replace(procsI, p, [p EXCEPT !.ver    = RedisTotalVer,
                                            !.status = IF RedisTotalVer = 0 THEN "ReadDB" ELSE "Respond"])
  /\ UNCHANGED << db, redis, procs, pub, userState >>

Load_ReadDB == \E p \in procsI :
  /\ p.status = "ReadDB"
  /\ procsI' = Replace(procsI, p, [p EXCEPT !.ver    = db.ver,
                                            !.status = "WriteRedis"])
  /\ UNCHANGED << db, redis, procs, pub, userState >>

Load_WriteRedis == \E p \in procsI :
  /\ p.status = "WriteRedis"
  /\ procsI' = Replace(procsI, p, [p EXCEPT !.status = "Respond"])
  /\ RedisWriteSnapshot(p.ver, TRUE, TRUE)
  /\ UNCHANGED << db, procs, pub, userState >>

Load_Respond == \E p \in procsI :
  /\ p.status = "Respond"
  /\ procsI' = Remove(procsI, p)
  /\ LET us == userState[p.user]
         r  == ApplyEvents[p.ver, us.future \ {p.ver}]
         us2 == [us EXCEPT !.ver = r[1], !.future = r[2], !.status = "active"]
     IN userState' = [userState EXCEPT ![p.user] = us2]
  /\ UNCHANGED << db, redis, procs, pub >>

Load ==
  \/ Load_ReadRedis
  \/ Load_ReadDB
  \/ Load_WriteRedis
  \/ Load_Respond

------------------------------------------------------------------------------------------------------------------------

ModRequest == \E u \in User : userState[u].status = "active"       \* For an online user
  /\ \E r \in Request : \A i \in User : r \notin userState[i].reqs \* get a unique req Id
    /\ userState' = [userState EXCEPT ![u].reqs = @ \union {r}]
    /\ procs'     = procs \union {[user |-> u, req |-> r, status |-> "ReadRedis", redisVer |-> 0, ver |-> 0]}
    /\ UNCHANGED << db, redis, pub, procsI >>

Respond_ReadRedis == procs /= {} /\ \E p \in procs :
  /\ p.status = "ReadRedis"
  /\ procs' = Replace(procs, p, [p EXCEPT !.ver      = RedisTotalVer,
                                          !.redisVer = RedisTotalVer,
                                          !.status   = IF RedisTotalVer > p.ver THEN "WriteDB" ELSE "ReadDB"])
  /\ UNCHANGED << db, redis, pub, userState, procsI >>

Respond_ReadDB == procs /= {} /\ \E p \in procs :
  /\ p.status = "ReadDB"
  /\ procs' = Replace(procs, p, [p EXCEPT !.ver = db.ver, !.status = "WriteRedis1"])
  /\ UNCHANGED << db, redis, pub, userState, procsI >>

Respond_WriteRedis1 == \E p \in procs :
  /\ p.status = "WriteRedis1"
  /\ LET Continue == procs' = Replace(procs, p, [p EXCEPT !.status = "WriteDB"])
         Retry    == procs' = Replace(procs, p, [p EXCEPT !.status = "ReadRedis"])
         WriteEvents ==
           LET firstEvent == p.redisVer + 1
               tryEvents  == firstEvent .. p.ver
               newEvents  == {e \in tryEvents : e > RedisTotalVer}
           IN IF redis.ver = 0 \/ firstEvent > RedisTotalVer + 1 \* Is there a missing event? Would this create a gap?
              THEN /\ UNCHANGED redis
                   /\ Retry
              ELSE /\ redis' = [redis EXCEPT !.events = @ \union newEvents]
                   /\ Continue
     IN \/ RedisWriteSnapshot(p.ver, Continue, Retry)
        \/ WriteEvents
  /\ UNCHANGED << db, pub, userState, procsI >>

Respond_WriteDB == procs /= {} /\ \E p \in procs :
  /\ p.status = "WriteDB"
  /\ \/ \* Request is valid
        /\ IF p.ver = db.ver
           THEN LET newVer == db.ver + 1
                IN /\ db'    = [ver |-> newVer]
                   /\ procs' = Replace(procs, p, [p EXCEPT !.status = "WriteRedis2", !.ver = newVer])
           ELSE \* DB has been updated without our knowledge; INSERT fails
                /\ procs' = Replace(procs, p, [p EXCEPT !.status = "ReadRedis"])
                /\ UNCHANGED db
        /\ UNCHANGED << redis, procsI, pub, userState >>
     \/ \* Request is invalid
        /\ procs' = Replace(procs, p, [p EXCEPT !.status = "Done"])
        /\ UNCHANGED << db, redis, procsI, pub, userState >>

Respond_WriteRedis2 == procs /= {} /\ \E p \in procs :
  /\ p.status = "WriteRedis2"
  /\ pub' = pub \union { <<p.user, p.ver>> }                \* Proc does this
                \union { <<u, p.ver>> : u \in OnlineUsers } \* Redis does this
  /\ \/ \* Send a snapshot to Redis
        IF p.ver > RedisTotalVer
        THEN redis' = [ver |-> p.ver, events |-> {}]
        ELSE UNCHANGED redis
     \/ \* Send an event to Redis
        IF p.ver = RedisPartialVer + 1
        THEN redis' = [redis EXCEPT !.events = @ \union {p.ver}]
        ELSE UNCHANGED redis
  /\ procs' = Replace(procs, p, [p EXCEPT !.status = "Done"])
  /\ UNCHANGED << db, procsI, userState >>

\* Responds to user
Respond_Done == procs /= {} /\ \E p \in procs :
  /\ p.status = "Done"
  /\ IF userState[p.user].status = "active"
     THEN userState' = [userState EXCEPT ![p.user].reqs = Remove(@, p.req)]
     ELSE UNCHANGED userState
  /\ procs' = Remove(procs, p)
  /\ UNCHANGED << db, redis, procsI, pub >>

ModRespond ==
  \/ Respond_ReadRedis
  \/ Respond_ReadDB
  \/ Respond_WriteRedis1
  \/ Respond_WriteDB
  \/ Respond_WriteRedis2
  \/ Respond_Done

------------------------------------------------------------------------------------------------------------------------

Publish ==
  LET RecvEvent(s, v) ==
        IF v <= s.ver
        THEN s
        ELSE LET r == ApplyEvents[s.ver, s.future \union {v}]
             IN [s EXCEPT !.ver = r[1], !.future = r[2]]
  IN
    /\ pub /= {}
    /\ \E <<u,v>> \in pub :
      /\ IF userState[u].status /= "offline" \* status=loading included because as soon as the websocket is established, the loading proc subscribes
         THEN userState' = [userState EXCEPT ![u] = RecvEvent(@, v)]
         ELSE UNCHANGED userState
      /\ pub' = Remove(pub, <<u,v>>)
      /\ UNCHANGED << db, redis, procs, procsI >>

\* This is the websocket being closed and not being restablished (i.e. user closes tab)
\* TODO: If the tab remains open on a disconnect, the client should reestablish a websocket and say where it's up to
UserDisconnect ==
  /\ IncludeUserDisconnect
  /\ \E u \in User : userState[u].status /= "offline"
    /\ userState' = [userState EXCEPT ![u] = OfflineUser]
    /\ pub'       = {usrEvt \in pub : usrEvt[1] /= u}
    /\ UNCHANGED << db, redis, procs, procsI >>

RedisEviction ==
  /\ \/ redis' = [redis EXCEPT !.ver = 0]
     \/ redis' = [redis EXCEPT !.events = {}]
  /\ UNCHANGED << db, procs, procsI, pub, userState >>

WebappDeath ==
  \* Start with a subset of users because each user communicates through a websocket which is tied to a specific worker
  \* Also remember the user isn't a ShipReq user; it's a browser tab, a session.
  /\ OnlineUsers /= {}
  /\ \E affectedUsers \in SUBSET(OnlineUsers) :
    /\ affectedUsers /= {}
    /\ userState' = [u \in User |-> IF u \in affectedUsers
                                    THEN [userState[u] EXCEPT !.online = FALSE, !.reqs = {}]
                                    ELSE userState[u]]
    /\ procs' = {p \in procs : p.user \notin affectedUsers}
    /\ pub' = {usrEvt \in pub : usrEvt[1] \notin affectedUsers}
    /\ UNCHANGED << db, redis, procsI >>

------------------------------------------------------------------------------------------------------------------------

ActionAct ==
  \/ UserConnect
  \/ ModRequest
  \/ RedisEviction
  \/ UserDisconnect
\*  \/ WebappDeath

ActionReact ==
  \/ Load
  \/ ModRespond
  \/ Publish

Action == ActionAct \/ ActionReact

Fairness ==
  /\ WF_vars(ModRequest)
\*  /\ SF_vars(Load)
\*  /\ UserConnect ~> Load_Respond
  /\ SF_vars(ModRespond)
  /\ SF_vars(Publish)

Spec == Init /\ [][Action]_<<vars>> \* /\ Fairness

THEOREM  Spec => [](TypeInvariants /\ DataInvariants)

------------------------------------------------------------------------------------------------------------------------

NothingInFlight ==
  /\ procs = {}
  /\ pub = {}
  /\ \A u \in User : userState[u].reqs = {}

AllUsersUpToDate ==
  \A u \in User :
    /\ userState[u].status = "active" => userState[u].ver = db.ver
    /\ ~userState[u].status = "loading"

CONSTANT MCVerLimit

MCSymmetry        == Permutations(User) \union Permutations(Request)
MCLimitReached    == db.ver >= MCVerLimit
MCDone            == MCLimitReached /\ NothingInFlight
MCFinalInvariants == MCDone => AllUsersUpToDate
MCContinue        == ~MCDone
MCAction          == (~MCLimitReached /\ ActionAct) \/ ActionReact
MCSpec            == Init /\ [][MCAction]_<<vars>> /\ Fairness

========================================================================================================================
