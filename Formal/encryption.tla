-------------------------------------------------- MODULE encryption --------------------------------------------------

(*
Client                  Server
  |                        |
  | NewProject             |
  |----------------------> |
  | <----------------------|
  |                 secret |
  |                        |
  | + keyKey               |
  | + dataKey              |
  |                        |
  | dataKey:data           |
  | keyKey:dataKey         |
  | keyKey:secret          |
  |----------------------> |


Client                  Server
  |                        |
  | keyKey                 |
  |                        |
  | ReadProject            |
  |----------------------> |
  | <----------------------|
  |          keyKey:secret |
  |                        |
  | secret                 |
  |----------------------> |
  |                     ok |
  | <----------------------|
  |           dataKey:data |
  |         keyKey:dataKey |
  |                        |
  | dataKey                |
  | data                   |
  |                        |
  |========================|
  |                        |
  | WriteProject           |
  |                        |
  | + data'                |
  |                        |
  | dataKey:data'          |
  |----------------------> |


Client                  Server
  |                        |
  | keyKey                 |
  |                        |
  | ReplaceKey             |
  |----------------------> |
  | <----------------------|
  |          keyKey:secret |
  |                        |
  | secret                 |
  |----------------------> |
  |                     ok |
  | <----------------------|
  |         keyKey:dataKey |
  |                secret' |
  |                        |
  | dataKey                |
  | + keyKey'              |
  |                        |
  | keyKey':dataKey        |
  | keyKey':secret'        |
  |----------------------> |
*)

EXTENDS Sequences, TLC

CONSTANTS Data,
          Keys,
          Secrets,
          Users
          
VARIABLES serverSeen,   \* Keys & secrets the server has ever had undecrypted access to
          userSeen,     \* Keys & secrets each user has ever had undecrypted access to
          pcReplaceKey,

          \* Server-side state:
          data,         \* User data (project content) decrypted with dataKey
          key,          \* dataKey decrypted with keyKey
          secret,       \* secret (undecrypted)
          secretE,      \* secret decrypted with keyKey
          oldSecrets    \* Previously used secrets (undecrypted)

vars == << serverSeen, userSeen, pcReplaceKey, data, key, secret, secretE, oldSecrets >>

UsedSecrets        == oldSecrets \union {secret}
UsersWithoutKeyKey == {u \in Users : key.key \notin userSeen[u]}
UsersWithKeyKey    == {u \in Users : key.key \in userSeen[u]}

-----------------------------------------------------------------------------------------------------------------------

TypeInvariants ==
  /\ serverSeen   \in SUBSET (Keys \union Secrets)
  /\ userSeen     \in [Users -> SUBSET (Keys \union Secrets)]
  /\ pcReplaceKey \in [Users -> Secrets \union {FALSE}]
  /\ data         \in [decrypted: {Data},  key: Keys] \* This is a blob which if decrypted with .key, would produce .decrypted
  /\ key          \in [decrypted: Keys,    key: Keys] \* This is a blob which if decrypted with .key, would produce .decrypted
  /\ secretE      \in [decrypted: Secrets, key: Keys]
  /\ secret       \in Secrets
  /\ oldSecrets   \in SUBSET Secrets

KeyKeyUnlocksDataKey ==
  key.decrypted = data.key

ValueInvariants ==
  /\ secretE.key = key.key      \* secretE decrypted by keyKey
  /\ secretE.decrypted = secret \* secret & secretE are the same secret
  \* debug
  \*/\ PrintT([serverSeen |-> serverSeen, userSeen |-> userSeen, pcReplaceKey |-> pcReplaceKey, data |-> data, key |-> key, secret |-> secret, secretE |-> secretE, oldSecrets |-> oldSecrets])

SanityChecksT ==
  /\ serverSeen \subseteq serverSeen'
  /\ \A u \in Users : userSeen[u] \subseteq userSeen'[u]

SanityChecks == [][SanityChecksT]_<<vars>>

-----------------------------------------------------------------------------------------------------------------------

UserSees(u, s) == userSeen' = [userSeen EXCEPT ![u] = @ \union s]
ServerSees(s)  == serverSeen' = serverSeen \union s

-----------------------------------------------------------------------------------------------------------------------

Init ==
  LET u       == CHOOSE u \in Users : TRUE
      dataKey == CHOOSE k \in Keys  : TRUE
      keyKey  == CHOOSE k \in Keys  : TRUE
  IN
    /\ secret       = CHOOSE s \in Secrets : TRUE
    /\ serverSeen   = {secret}
    /\ pcReplaceKey = [i \in Users |-> FALSE]
    /\ data         = [decrypted |-> Data,    key |-> dataKey]
    /\ key          = [decrypted |-> dataKey, key |-> keyKey]
    /\ secretE      = [decrypted |-> secret,  key |-> keyKey]
    /\ userSeen     = [i \in Users |-> IF i = u THEN {dataKey, keyKey, secret} ELSE {}]
    /\ oldSecrets   = {}

ReadProject(u) ==
  /\ \/ key.key \in userSeen[u]
     \/ secret \in userSeen[u] /\ data.key \in userSeen[u]
  /\ UserSees(u, {data.key, secret})
  /\ UNCHANGED << serverSeen, pcReplaceKey, data, key, secret, secretE, oldSecrets >>

\* Users can share secrets between themselves offline
\* In terms of keyKeys, that's expected and recommended.
\* In terms of secrets & dataKeys, those are hacking attempts.
UsersShareSecrets ==
  \E u1 \in Users :
  \E s  \in userSeen[u1] :
  \E u2 \in Users :
    /\ u1 /= u2
    /\ s \notin userSeen[u2]
    /\ UserSees(u2, {s})
    /\ UNCHANGED << serverSeen, pcReplaceKey, data, key, secret, secretE, oldSecrets >>

ReplaceKey1(u) ==
  LET seen      == userSeen[u]
      hasKeyKey == key.key \in seen
      secret2   == CHOOSE s \in Secrets : s \notin UsedSecrets
      seenDK2   == IF hasKeyKey THEN {data.key} ELSE {}
  IN
    /\ \/ hasKeyKey
       \/ secret \in seen /\ data.key \in seen
    /\ UserSees(u, {secret2} \union seenDK2)
    /\ pcReplaceKey' = [pcReplaceKey EXCEPT ![u] = secret2]
    /\ ServerSees({secret2})
    /\ UNCHANGED << data, key, secret, secretE, oldSecrets >>

ReplaceKey2(u) ==
  LET secret2 == pcReplaceKey[u]
  IN
    /\ secret2 /= FALSE
    /\ \E keyKey2 \in Keys :
       \E dataKey2 \in Keys : \* Either user decrypts actual dataKey using keyKey (expected), or attacker uses bullshit
        /\ UserSees(u, {keyKey2, dataKey2, secret2})
        /\ secret'       = secret2
        /\ secretE'      = [decrypted |-> secret2,  key |-> keyKey2]
        /\ key'          = [decrypted |-> dataKey2, key |-> keyKey2]
        /\ oldSecrets'   = oldSecrets \union {secret}
        /\ pcReplaceKey' = [pcReplaceKey EXCEPT ![u] = FALSE]
        /\ UNCHANGED << serverSeen, data >>

Next ==
  \/ UsersShareSecrets
  \/ \E u \in Users :
    \/ ReadProject(u)
    \/ ReplaceKey1(u)
    \/ ReplaceKey2(u)
  
-----------------------------------------------------------------------------------------------------------------------

SafeFromServer ==
  LET CanDecryptData == data.key \in serverSeen
      CanDecryptKey  == key.key \in serverSeen
  IN ~(CanDecryptData \/ CanDecryptKey)

SafeFromUsersWithoutKeyKey ==
  /\ \A u \in UsersWithoutKeyKey :
    /\ ~ENABLED(ReadProject(u))

OpenToUsersWithKeyKey ==
  /\ \A u \in UsersWithKeyKey :
    /\ ENABLED(ReadProject(u))

-----------------------------------------------------------------------------------------------------------------------

Spec == Init /\ [][Next]_<<vars>>

========================================================================================================================
