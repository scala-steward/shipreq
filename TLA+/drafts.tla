---------------------------------------------------- MODULE drafts ----------------------------------------------------
(*
What's does this spec provide?
==============================
- Drafts should eventually propagate to all live tabs
- Drafts are never lost unless they're merged (manually or automatically), or completed by a user (whether aborted or committed)
- Drafts are removed from all storage locations once obsolete
- Users are prompted to keep a tab open iff we can't guarantee the draft won't be lost
- Reliability in the face of failures such as
  - network packet loss
  - machines crashing or dying without warning

See https://shipreq.com/project/d6My#/reqs/DE-5


How does it work?
=================

+--------------------------------------------------+
|                    Browser                       |
|                                                  |
|                                                  |
|  ------------+                                   |
|  | indexedDB |                                   |
|  +-----------+                                   |
|        ↕                                         |                     +----------------+
|   +----------+                       +-------+   |                     |                |
|   |          |     +-----------+     |       |   |   +-----------+     |     Remote     |
|   |  Worker  |<--->|  Network  |<--->|  Tab  |<----->|  Network  |<--->|                |
|   |          |     +-----------+     |       |   |   +-----------+     |   (database)   |
|   +----------+                       +-------+   |                     |                |
|        ↕                                         |                     +----------------+
| +--------------+                                 |
| | localStorage |                                 |
| +--------------+                                 |
+--------------------------------------------------+

Important notes
===============

- WW time always starts at 1
- Provenance maps have keys for all workers but when the value=0 it means the K:V entry doesn't really exist in the map

TODO
====
*)

EXTENDS FiniteSets, Naturals, Sequences, TLC, Util

CONSTANT Browser
CONSTANT BrowserSrcAsync
CONSTANT BrowserSrcSync
CONSTANT Tab
CONSTANT Worker

CONSTANT MCBrowserStorageAlwaysAvailable

ASSUME & IsFiniteSet(Browser)
       & IsFiniteSet(BrowserSrcAsync)
       & IsFiniteSet(BrowserSrcSync)
       & IsFiniteSet(Tab)
       & IsFiniteSet(Worker)
       & Cardinality(Worker) >= Cardinality(Browser) \* 2w in 1b = diff versions, 1w in 2b doesn't make sense
       & MCBrowserStorageAlwaysAvailable \in BOOLEAN

MCSymmetry ==
  SymmetrySets(<<
    Browser,
    BrowserSrcAsync,
    BrowserSrcSync,
    Tab,
    Worker
  >>)

VARIABLE browsers
VARIABLE network
VARIABLE remote
VARIABLE tabs
VARIABLE workers

vars == << browsers, network, remote, tabs, workers >>

state == [
  browsers |-> browsers,
  network  |-> network,
  remote   |-> remote,
  tabs     |-> tabs,
  workers  |-> workers]

LogStates ==
  Log(state)

\* ███████████████████████████████████████████████████████████████████████████████████████████████████
\* Types

Provenance  == [Worker -> Nat]                               \* i.e. Map[WorkerId, Time]
Draft       == [worker: Worker, time: Nat, prov: Provenance] \* no need to include draft content
Drafts      == SUBSET Draft                                  \* i.e. Set[Draft]
DraftsNE    == Drafts -- {{}}                                \* i.e. NonEmptySet[Draft]

clean          == "clean"
conflicted     == "conflicted"
dirty          == "dirty"
live           == "live"
nonExistant    == "-"
server         == "server"
loading        == "loading"
Remote         == "Remote"
syncTW         == "sync:T->W"
syncWT         == "sync:W->T"
syncTR         == "sync:T->R"
syncRT         == "sync:R->T"
RemoteStoreCmd == "RemoteStoreCmd"
ackRT          == "ack:R->T"
ackTW          == "ack:T->W"

Msg == [
  type   : {syncTW},
  from   : Tab,
  to     : Worker,
  drafts : Drafts,
  newEdit: Option(Provenance)
] ++ [
  type   : {syncWT},
  from   : Worker,
  to     : Tab,
  drafts : Drafts,
  newEdit: Option(Draft)
] ++ [
  type   : {syncTR},
  from   : Tab,
  to     : {Remote},
  drafts : DraftsNE,
  time   : Nat
] ++ [
  type   : {RemoteStoreCmd},
  from   : Worker,
  to     : Tab,
  drafts : Drafts,
  time   : Nat
] ++ [
  type   : {ackRT},
  from   : {Remote},
  to     : Tab,
  time   : Nat
] ++ [
  type   : {ackTW},
  from   : Tab,
  to     : Worker,
  time   : Nat
] ++ [
  type   : {syncRT},
  from   : {Remote},
  to     : Tab,
  drafts : DraftsNE
]

NetworkState ==
  Seq(Msg) \* i.e. List[Msg]

BrowserSrc ==
  BrowserSrcAsync ++ BrowserSrcSync

BrowserState ==
  [BrowserSrc -> Option(Drafts)] \* None means not supported by browser

AnySrc ==
  BrowserSrc ++ {Remote}

ActiveTabStatus ==
  {clean, dirty, conflicted}

TabState ==
  [ status: {nonExistant}] ++
  [
    status  : {loading},
    worker  : Worker,
    drafts  : Drafts,
    awaiting: SUBSET AnySrc
  ] ++
  [
    status: {clean},
    worker: Worker
  ] ++
  [
    status     : {dirty},
    worker     : Worker,
    draft      : Option(Draft), \* last known draft for editor state
    localChange: BOOLEAN        \* editor has change that hasn't been sent to WW yet
  ] ++
  [
    status     : {conflicted},
    worker     : Worker,
    drafts     : { ds \in Drafts : Cardinality(ds) > 1 },
    localChange: BOOLEAN \* editor has change that hasn't been sent to WW yet
  ]

WorkerState ==
  [status: {nonExistant}] ++
  [
    status        : {live},
    browser       : Browser,
    time          : Nat,
    drafts        : Drafts,
    remoteSyncedTo: Nat,
    awaitingAck   : Option(Nat)
  ]

TypeInvariantsBrowsers == browsers \in [Browser -> BrowserState]
TypeInvariantsNetwork  == network  \in NetworkState
TypeInvariantsRemote   == remote   \in Drafts
TypeInvariantsTabs     == tabs     \in [Tab -> TabState]
TypeInvariantsWorkers  == workers  \in [Worker -> WorkerState]

\* ███████████████████████████████████████████████████████████████████████████████████████████████████
\* Data

StorageInvariants(s) ==
  & Assert1(
      Cardinality(s) = Cardinality({d.worker : d \in s}),
      "Duplicate drafts/worker:", s)
  & \A d \in s: Assert1(
      d.prov[d.worker] = 0,
      "Draft contains itself in its own provenance:", d)

DataInvariantsBrowsers ==
  \A b \in Browser :
    LET bs == browsers[b]
    IN \A src \in BrowserSrc:
      bs[src].isEmpty | StorageInvariants(bs[src].get)

DataInvariantsNetwork ==
  TRUE
  \* \A i \in DOMAIN network :
  \*   LET msg == network[i]
  \*       to == msg.to
  \*   IN
  \*     & to \in Worker => workers[to].status = live
  \*     & to \in Tab => tabs[to].status \in {clean, dirty, conflicted}

DataInvariantsRemote ==
  StorageInvariants(remote)

DataInvariantsTabs ==
  \* PrintT(browsers)
  TRUE

DataInvariantsWorkers ==
  \A w \in Worker :
    LET ws == workers[w]
    IN ws.status = live =>
        & ws.time > 0
        & ws.remoteSyncedTo <= ws.time
        & ~ws.awaitingAck.isEmpty =>
          & ws.awaitingAck.get > ws.remoteSyncedTo
          & ws.awaitingAck.get <= ws.time
        & network = <<>> => ws.awaitingAck.isEmpty

Init ==
  & network    = <<>>
  & remote     = {}
  & tabs       = [t \in Tab |-> [status |-> nonExistant]]
  & workers    = [w \in Worker |-> [status |-> nonExistant]]
  & IF MCBrowserStorageAlwaysAvailable
    THEN browsers = [b \in Browser |-> [s \in BrowserSrc |-> Some({})]]
    ELSE browsers \in [Browser -> [BrowserSrc -> NoneAndSome({})]]

\* ███████████████████████████████████████████████████████████████████████████████████████████████████
\* Functions

Connected(tab, worker) ==
  & tabs[tab].status != nonExistant
  & tabs[tab].worker = worker

WorkerTabs(w) ==
  { t \in Tab : Connected(t, w) }

ActiveTabs ==
  { t \in Tab : tabs[t].status \in ActiveTabStatus }

RemoteConnectedTabs ==
  LET statuses == ActiveTabStatus ++ {loading}
  IN { t \in Tab : tabs[t].status \in statuses }

TabDrafts(t) ==
  LET ts == tabs[t]
      s  == ts.status
  IN CASE s = nonExistant -> {}
       [] s = loading     -> ts.drafts
       [] s = clean       -> {}
       [] s = dirty       -> OptionToSet(ts.draft)
       [] s = conflicted  -> ts.drafts

TabHasLocalChange(t) ==
  LET ts == tabs[t]
      s  == ts.status
  IN CASE s = nonExistant -> FALSE
       [] s = loading     -> FALSE
       [] s = clean       -> FALSE
       [] s = dirty       -> ts.localChange
       [] s = conflicted  -> ts.localChange

NewTabState(w, prunedDrafts, localChange) ==
  LET cleanState    == [worker |-> w, status |-> clean]
      dirtyState(d) == [worker |-> w, status |-> dirty, draft |-> Some(d), localChange |-> localChange]
      conflictState == [worker |-> w, status |-> conflicted, drafts |-> prunedDrafts, localChange |-> localChange]
      soleDraft     == SetSoleElement(prunedDrafts)
  IN
    IF prunedDrafts = {} THEN
      cleanState
    ELSE IF ~soleDraft.isEmpty THEN
      dirtyState(soleDraft.get)
    ELSE
      conflictState

TabStateWithDrafts(t, drafts) ==
  LET ts     == tabs[t]
      s      == ts.status
      lc     == TabHasLocalChange(t)
  IN CASE s = nonExistant       -> ts
       [] s = loading           -> [ts EXCEPT !.drafts = drafts]
       [] s \in ActiveTabStatus -> NewTabState(ts.worker, drafts, lc)

\* Set[(Browser, BrowserSrc)]
AvailableBrowserStores ==
  { x \in Browser \X BrowserSrc : ~browsers[x[1]][x[2]].isEmpty }

ActiveWorkers ==
  { w \in Worker : workers[w].status != nonExistant }

\* Set[Storage]
AllStores ==
  LET draftsB == { browsers[x[1]][x[2]].get : x \in AvailableBrowserStores }
      draftsT == { TabDrafts(t) : t \in ActiveTabs }
      draftsW == { workers[w].drafts : w \in ActiveWorkers }
      draftsR == { remote }
  IN draftsB ++ draftsT ++ draftsW ++ draftsR

\* Set[Draft]
AllDrafts ==
  UNION AllStores

SendMsg(msg) ==
  network' = Append(network, msg)

RecvMsg(i) ==
  network' = RemoveAt(network, i)

RecvResp(recv, resp) ==
  network' = Append(RemoveAt(network, recv), resp)

NewDraft(w, prevProv) ==
  [
    worker |-> w,
    time   |-> workers[w].time,
    prov   |-> [prevProv EXCEPT ![w] = 0]
  ]

NoProv ==
  [w \in Worker |-> 0]

MergeProvs(p1, p2) ==
  [w \in Worker |-> Max[p1[w], p2[w]]]

AddProv(draft, prov) ==
  [draft EXCEPT !.prov = MergeProvs(@, prov)]

AddSelfToOwnProv(draft) ==
  LET w == draft.worker
  IN  [draft EXCEPT !.prov[w] = draft.time]

(* NOTE: Doesn't prune *)
AddDraft(storage, draft) ==
  LET sibling == SetFind(storage, LAMBDA d: d.worker = draft.worker)
  IN IF sibling.isEmpty THEN
       storage ++ {draft}
     ELSE
      LET s   == sibling.get
          new == IF s.time > draft.time THEN s ELSE draft
          old == IF s.time > draft.time THEN draft ELSE s
      IN IF s = draft
         THEN storage
         ELSE (storage -- {s}) ++ {AddProv(new, old.prov)}

(* NOTE: Doesn't prune *)
AddDrafts(storage, drafts) ==
  SetFold(drafts, storage, AddDraft)

(*
Prunes according to provenance. Eg.
  {
    (w1:4, prov: {w1:0, w2:3, w3:0})
    (w2:3, prov: {w1:0, w2:0, w3:1}) <-- prunable cos w2:3 <= w2:3 in prov above
    (w3:2, prov: {w1:0, w2:0, w3:0})
  }
is pruned to
  {
    (w1:4, prov: {w1:0, w2:3, w3:1}) <-- inherits w3:1 from removed draft's provenance
    (w3:2, prov: {w1:0, w2:0, w3:0})
  }
or if w1 and w3 drafts have the same content:
  {
    (w1:4, prov: {w1:0, w2:3, w3:2})
  }
*)
RECURSIVE PruneByProv(_)
PruneByProv(ds) ==
  LET f2(d1, d2) ==
        LET pt     == d1.prov[d2.worker]
            byProv == d1.worker != d2.worker & pt > 0 & d2.time < pt
            bySrc  == d1.worker = d2.worker & d1.time > d2.time
        IN SomeWhen(bySrc | byProv, <<d1, d2>>)
      f(d1) == SetCollectFirst(ds, LAMBDA d2: f2(d1, d2))
      match == SetCollectFirst(ds, f)
  IN IF match.isEmpty
     THEN ds
     ELSE LET d1  == match.get[1]
              d2  == match.get[2]
              d   == AddProv(d1, d2.prov)
              ds2 == (ds -- {d1, d2}) ++ {d}
          IN PruneByProv(ds2)

\* Returns a set of possible outcomes
PruneByEq(ds) ==
  LET equalSets    == { x \in SUBSET(ds) : Cardinality(x) > 1 } \* Set[Set[Drafts]]
      merge(x, y)  == AddProv(x, AddSelfToOwnProv(y).prov)
      mergeAll(es) == SetReduce(es, merge)
  IN { (ds -- es) ++ {mergeAll(es)} : es \in equalSets }

\* Returns a set of possible outcomes
Prune(drafts) ==
  PruneByEq(drafts) ++ {PruneByProv(drafts)}

\* OnEdit(w) ==
\*   LET
\*     ws        == workers[w]
\*     t         == ws.time
\*     lastEdit2 == IF ws.editor.status = closed THEN ws.lastEdit ELSE t
\*     prevProv  == IF ws.editor.status = closed THEN NoProv ELSE ws.editor.draft.prov
\*     draft2    == NewDraft(w, prevProv)
\*     editor2   == [status |-> dirty, draft |-> draft2]
\*   IN [workers EXCEPT ![w] = [ws EXCEPT
\*         !.time      = t + 1,
\*         !.editor    = editor2,
\*         !.lastEdit  = lastEdit2,
\*         !.syncQueue = Store(@, draft2)
\*       ]]

\* ------------------------------------------------------------------------------------------------------------------------
\* Actions

RemoteRecvDrafts ==
  LET i == SeqIndexOf(network, LAMBDA m: m.type = syncTR)
  IN
    & i != 0
    & LET msg == network[i]
          resp == [
            type |-> ackRT,
            from |-> Remote,
            to   |-> msg.from,
            time |-> msg.time
          ]
          broadcastTo(tab, ds) == [
            type   |-> syncRT,
            from   |-> Remote,
            to     |-> tab,
            drafts |-> ds
          ]
          otherTabs      == { t \in RemoteConnectedTabs : t != msg.from }
          broadcasts(ds) == SetFold(otherTabs, <<>>, LAMBDA q,t: q \o <<broadcastTo(t, ds)>>)
          result(ds)     == <<ds, <<resp>> \o broadcasts(ds)>>
          dss            == Prune(AddDrafts(remote, msg.drafts))
          results        == { result(ds) : ds \in dss }
      IN
        & remote' \in { r[1] : r \in results }
        & network' \in { RemoveAt(network, i) \o r[2] : r \in results }
        & UNCHANGED << browsers, tabs, workers >>

TabRecvDraftsFromRemote ==
  LET i == SeqIndexOf(network, LAMBDA m: m.type = syncRT)
  IN
    & i != 0
    & LET msg       == network[i]
          t         == msg.to
          ts        == tabs[t]
          w         == ts.worker
          dss       == Prune(AddDrafts(TabDrafts(t), msg.drafts))
          newTS(ds) == [tabs EXCEPT ![t] = TabStateWithDrafts(t, ds)]
          net1      == RemoveAt(network, i)
          msgWW(ds) == [
                         type    |-> syncTW,
                         from    |-> t,
                         to      |-> w,
                         drafts  |-> ds,
                         newEdit |-> IF TabHasLocalChange(t) & ts.status = dirty
                                     THEN Some(ts.draft)
                                     ELSE None
                       ]
          newN(ds)  == IF ds = TabDrafts(t) THEN net1 ELSE Append(net1, msgWW(ds))
          results   == { <<newTS(ds), newN(ds)>> : ds \in dss }
      IN
        & tabs'    \in { r[1] : r \in results }
        & network' \in { r[2] : r \in results }
        & UNCHANGED << browsers, remote, workers >>

TabRecvDraftsFromWorker ==
  LET i == SeqIndexOf(network, LAMBDA m: m.type = syncWT)
  IN
    & i != 0
    & LET msg == network[i]
          t   == msg.to
          ts  == tabs[t]
          w   == ts.worker
          dss == Prune(AddDrafts(TabDrafts(t), msg.drafts))
          localChanges ==
            IF ~msg.newEdit.isEmpty & TabHasLocalChange(t) THEN
              BOOLEAN \* Maybe the new draft clears out the status, maybe there are more changes since
            ELSE
              {FALSE}
          results ==
            UNION {{
              [tabs EXCEPT ![t] = NewTabState(w, ds, ls)]
              : ls \in localChanges }
              : ds \in dss }
      IN
        & RecvMsg(i)
        & tabs' \in results
        & UNCHANGED << browsers, remote, workers >>

TabLoad ==
  \E t \in Tab:
    & tabs[t].status = loading
    & tabs[t].awaiting != {}
    & UNCHANGED << browsers, network, remote, workers >>
    & LET ts == tabs[t]
          w  == ts.worker
          b  == workers[w].browser
          bs == browsers[b]
          Attempt(src, srcDrafts) ==
            IF src \notin ts.awaiting
            THEN {}
            ELSE LET drafts2s  == Prune(AddDrafts(ts.drafts, srcDrafts))
                     awaiting2 == ts.awaiting -- {src}
                     ts2(ds2)  == [ts EXCEPT !.drafts = ds2, !.awaiting = awaiting2]
                 IN {[tabs EXCEPT ![t] = ts2(ds2)] : ds2 \in drafts2s }
          AttemptOption(src, o) ==
            IF o.isEmpty
            THEN {[tabs EXCEPT ![t].awaiting = @ -- {src}]}
            ELSE Attempt(src, o.get)
          browserAttempts ==
            UNION { AttemptOption(src, bs[src]) : src \in BrowserSrc }
      IN tabs' \in (browserAttempts ++ Attempt(Remote, remote))

TabNew ==
  \E t \in Tab:
    & tabs[t].status = nonExistant
    & UNCHANGED << browsers, network, remote >>
    & \E w \in Worker:
      & \* Connect to worker
        | \* New worker
          & workers[w].status = nonExistant
          & \E b \in Browser:
            & workers' = [workers EXCEPT ![w] = [
                  status         |-> live,
                  browser        |-> b,
                  time           |-> 1,
                  drafts         |-> {}, \* TODO load in stages just like TabNew
                  remoteSyncedTo |-> 1,
                  awaitingAck    |-> None
                ]]
        | \* Existing worker
          & workers[w].status = live
          & UNCHANGED workers
      & tabs' = [tabs EXCEPT ![t] = [
          status   |-> loading,
          worker   |-> w,
          drafts   |-> {},
          awaiting |-> AnySrc
        ]]

TabStart ==
  \E t \in Tab:
    LET ts == tabs[t]
        w  == ts.worker
    IN
      & ts.status = loading
      & ts.awaiting = {}
      & tabs' = [tabs EXCEPT ![t] = NewTabState(w, ts.drafts, FALSE)]
      & IF ts.drafts = {}
        THEN UNCHANGED network
        ELSE SendMsg([
              type    |-> syncTW,
              from    |-> t,
              to      |-> w,
              drafts  |-> ts.drafts,
              newEdit |-> None
             ])
      & UNCHANGED << browsers, remote, workers >>

TabRecvRemoteStoreCmd ==
  LET i == SeqIndexOf(network, LAMBDA m: m.type = RemoteStoreCmd)
  IN
    & i != 0
    & LET msg       == network[i]
          t         == msg.to
          dss       == Prune(AddDrafts(TabDrafts(t), msg.drafts))
          msgW      == [
                         type    |-> ackTW,
                         from    |-> t,
                         to      |-> msg.from,
                         time    |-> msg.time
                       ]
          msgR(ds)  == [
                         type    |-> syncTR,
                         from    |-> t,
                         to      |-> Remote,
                         drafts  |-> ds,
                         time    |-> msg.time
                       ]
          saveT(ds) == [tabs EXCEPT ![t] = TabStateWithDrafts(t, ds)]
          send(res) == Append(RemoveAt(network, i), res)
          resultsNE == { <<saveT(ds), send(msgR(ds))>> : ds \in dss }
      IN
        IF dss = {} THEN
          & RecvResp(i, msgW)
          & UNCHANGED << browsers, workers, remote, tabs >>
        ELSE
          & tabs'    \in { r[1] : r \in resultsNE }
          & network' \in { r[2] : r \in resultsNE }
          & UNCHANGED << browsers, workers, remote >>

TabSendChangesToWorker ==
  \E t \in Tab:
    LET ts == tabs[t]
    IN
      & ts.status = dirty
      & ts.localChange
      & tabs' = [tabs EXCEPT ![t].localChange = FALSE]
      & SendMsg([
          type    |-> syncTW,
          from    |-> t,
          to      |-> ts.worker,
          drafts  |-> {},
          newEdit |-> SomeWhen(ts.localChange, IF ts.draft.isEmpty THEN NoProv ELSE ts.draft.get.prov)
        ])
      & UNCHANGED << browsers, workers, remote >>

UserEditClean ==
  \E t \in Tab:
    LET ts  == tabs[t]
        w   == ts.worker
        ts2 == [worker |-> w, status |-> dirty, draft |-> None, localChange |-> TRUE]
    IN
      & ts.status = clean
      & tabs' = [tabs EXCEPT ![t] = ts2]
      & UNCHANGED << browsers, workers, network, remote >>

\* No need for this because
\* 1. In TabRecvDraftsFromWorker we handle the case that a local change has been made after sending it to WW
\* 2. Enabling makes it very hard to keep the model space finite
\* 3. The only thing we're missing is an editor sending multiple revisions of a draft to WW before getting a result
\*    which is extremely low probability (if possible at all) PLUS we can handle that logic easily enough
\*    outside of the model. Having it in the model shouldn't change anything.
UserEditDirty ==
  FALSE
\*   \E t \in Tab:
\*     & tabs[t].status = dirty
\*     & ~tabs[t].localChange
\*     & tabs' = [tabs EXCEPT ![t].localChange = TRUE]
\*     & UNCHANGED << browsers, workers, network, remote >>

WorkerRecvChanges ==
  LET i == SeqIndexOf(network, LAMBDA m: m.type = syncTW)
  IN
    & i != 0
    & LET msg      == network[i]
          w        == msg.to
          ws       == workers[w]
          t2       == IF msg.newEdit.isEmpty THEN ws.time ELSE ws.time + 1
          new      == OptionMap(msg.newEdit, LAMBDA n: NewDraft(w, n))
          dss      == Prune(AddDrafts(ws.drafts, AddDrafts(msg.drafts, OptionToSet(new))))
          ws2(ds)  == [workers EXCEPT ![w].drafts = ds, ![w].time = t2]
          msgs(ds) == IF ds = ws.drafts THEN
                        {}
                      ELSE
                        { [
                            type    |-> syncWT,
                            from    |-> w,
                            to      |-> t,
                            drafts  |-> ds,
                            newEdit |-> new
                          ] : t \in WorkerTabs(w)
                        }
          results == { <<ws2(ds), msgs(ds)>> : ds \in dss }
          network2 == RemoveAt(network, i)
      IN
        & workers' \in { r[1] : r \in results }
        & network' \in { SetFold(r[2], network2, Append) : r \in results }
        & UNCHANGED << browsers, remote, tabs >>

\* This happens periodically without a trigger event
WorkerSyncWithBrowserStorage ==
  \E w \in Worker:
    LET ws == workers[w]
        b  == workers[w].browser
        bs == browsers[b]
        Attempt(src) ==
          IF bs[src].isEmpty | bs[src].get = ws.drafts THEN
            {}
          ELSE
            LET dss == Prune(AddDrafts(ws.drafts, bs[src].get))
                ws2(ds) == [workers EXCEPT ![w].drafts = ds]
                bs2(ds) == [browsers EXCEPT ![b][src] = Some(ds)]
            IN
              \* IF ~Log([WW |-> ws.drafts, BS |-> bs[src].get, RES |-> dss]) THEN {} ELSE
              { <<ws2(ds), bs2(ds)>> : ds \in dss }
        results == UNION { Attempt(s) : s \in BrowserSrc }
    IN
      & workers[w].status = live
      & workers'  \in { r[1] : r \in results }
      & browsers' \in { r[2] : r \in results }
      & UNCHANGED << remote, network, tabs >>

\* TODO Track online/offline status of tabs
\* TODO Assumes that awaitingAck will always be responded to (for the sake of model checking)
WorkerSendRemoteStoreCmd ==
  \E w \in Worker:
    LET ws == workers[w]
    IN
      & ws.status = live
      & ws.awaitingAck.isEmpty
      & ws.remoteSyncedTo < ws.time
      & ws.drafts != {}
      & LET t   == CHOOSE t \in WorkerTabs(w) : TRUE \* TODO CHOOSE or \E?
            cmd == [
              type   |-> RemoteStoreCmd,
              from   |-> w,
              to     |-> t,
              drafts |-> ws.drafts,
              time   |-> ws.time
            ]
        IN
          & SendMsg(cmd)
          & workers' = [workers EXCEPT ![w].awaitingAck = Some(ws.time)]
          & UNCHANGED << browsers, remote, tabs >>

TabRecvRemoteAck ==
  LET i == SeqIndexOf(network, LAMBDA m: m.type = ackRT)
  IN
    & i != 0
    & LET msg    == network[i]
          t      == msg.to
          newMsg == [
            type |-> ackTW,
            from |-> t,
            to   |-> tabs[t].worker,
            time |-> msg.time
          ]
      IN
        & RecvResp(i, newMsg)
        & UNCHANGED << browsers, remote, workers, tabs >>

WorkerRecvRemoteAck ==
  LET i == SeqIndexOf(network, LAMBDA m: m.type = ackTW)
  IN
    & i != 0
    & LET msg == network[i]
          w   == msg.to
          ws  == workers[w]
          t   == Max[ws.remoteSyncedTo, msg.time]
      IN
        & Assert1(ws.awaitingAck = Some(msg.time), "Worker not awaiting the msg", [ws |-> ws, msgTime |-> msg.time])
        & workers' = [workers EXCEPT ![w].remoteSyncedTo = t, ![w].awaitingAck = None]
        & RecvMsg(i)
        & UNCHANGED << browsers, remote, tabs >>

\* \* Will websockets periodically push? Will workers request?
\* \* As far as the spec goes it doesn't matter.
\* RemoteSend ==
\*   & remote != {}
\*   & \E w \in Worker:
\*     & workers[w].status = live
\*     & ~(\E i \in DOMAIN network : ~network[i].toSvr & network[i].worker = w) \* Don't re-send if msg already on the way
\*     & SendMsg([worker |-> w, toSvr |-> FALSE, drafts |-> remote])
\*     & UNCHANGED << browsers, workers, remote >>

\* ███████████████████████████████████████████████████████████████████████████████████████████████████
\* Spec

Next ==
  | RemoteRecvDrafts
  | TabLoad
  | TabNew
  | TabRecvDraftsFromRemote
  | TabRecvDraftsFromWorker
  | TabRecvRemoteAck
  | TabRecvRemoteStoreCmd
  | TabSendChangesToWorker
  | TabStart
  | UserEditClean
  | WorkerRecvChanges
  | WorkerRecvRemoteAck
  | WorkerSendRemoteStoreCmd
  | WorkerSyncWithBrowserStorage

Fairness ==
  & SF_<<vars>>(RemoteRecvDrafts)
  & SF_<<vars>>(TabLoad)
  \* & SF_<<vars>>(TabNew)
  & SF_<<vars>>(TabRecvDraftsFromRemote)
  & SF_<<vars>>(TabRecvDraftsFromWorker)
  & SF_<<vars>>(TabRecvRemoteAck)
  & SF_<<vars>>(TabRecvRemoteStoreCmd)
  & SF_<<vars>>(TabSendChangesToWorker)
  & SF_<<vars>>(TabStart)
  \* & SF_<<vars>>(UserEditClean)
  & SF_<<vars>>(WorkerRecvChanges)
  & SF_<<vars>>(WorkerRecvRemoteAck)
  & SF_<<vars>>(WorkerSendRemoteStoreCmd)
  & SF_<<vars>>(WorkerSyncWithBrowserStorage)

Spec == Init & [][Next]_<<vars>> & Fairness

\* Have all mechanical processes stopped such that the world is in a stable state that will stutter until
\* the user does something.
IsStable ==
  & network = <<>>
  & ~ENABLED(TabLoad)
  & ~ENABLED(TabSendChangesToWorker)
  & ~ENABLED(TabStart)
  & ~ENABLED(WorkerSendRemoteStoreCmd)
  & ~ENABLED(WorkerSyncWithBrowserStorage)
  \* & Log(state)

Liveness ==
  []<>IsStable \* We always stablise eventually
  \* <>[]IsStable \* We end in a stable state

StableInvariants ==
  IsStable =>

    & Assert1(
      \A t \in Tab : ~TabHasLocalChange(t),
      "Local changes aren't stored", tabs)

    & Assert1(
      Cardinality(AllStores) <= 1,
      "Drafts are not eventually-consistent", [
          AB     |-> AvailableBrowserStores,
          AW     |-> ActiveWorkers,
          AT     |-> ActiveTabs,
          Stores |-> AllStores
        ])

    & Assert1(
      remote = AllDrafts,
      "Drafts are not stored remotely", [all |-> AllDrafts, remote |-> remote])

========================================================================================================================
