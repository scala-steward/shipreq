---------------------------------------------------- MODULE drafts ----------------------------------------------------

(*
What's the point of this?
=========================

- ensure that all local drafts propagate and are eventually consistent

*)

EXTENDS FiniteSets, Naturals, Sequences, TLC

CONSTANTS Tab,
          Worker

MCSymmetry == Permutations(Tab) \union Permutations(Worker)

ASSUME /\ IsFiniteSet(Worker)

VARIABLES ts, \* tab states
          ws \* worker states

vars == << ts, ws >>

varDesc == [
  ts |-> ts,
  ws |-> ws
]

NonExistant == "NonExistant"

TabState ==
  {NonExistant}
  \* worker: W, editor: Option[D]

WorkerState ==
  {NonExistant}

TypeInvariants ==
  /\ ts \in [Tab -> TabState]
  /\ ws \in [Worker -> WorkerState]

------------------------------------------------------------------------------------------------------------------------

Init ==
  /\ ts = [t \in Tab |-> NonExistant]
  /\ ws = [w \in Worker |-> NonExistant]

DataInvariants ==
  /\ PrintT(varDesc)

------------------------------------------------------------------------------------------------------------------------

(* actions *)

------------------------------------------------------------------------------------------------------------------------

Next ==
  FALSE

Spec == Init /\ [][Next]_<<vars>>

Live ==
  TRUE

========================================================================================================================
