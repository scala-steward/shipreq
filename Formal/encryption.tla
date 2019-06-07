-------------------------------------------------- MODULE encryption --------------------------------------------------

(* PROTOCOL
   - If users send their key to the server to assert it's valid, they'll violate SafeFromServer
   - Maybe server sends [encrypted: 'secret123', key: K] and if the user sends back 'secret123' they we know they have a valid key
     'secret123' would be initially encrypted by client in NewProject and send as plain text to server
     'secret123' would be to be tracked in a Seen variable to ensure that they can't use it later to avoid their key becoming invalid
     If every key change requires a new secret then I think we're good - would have to retain all old secrets and ensure no reuse on key change

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
          
VARIABLES serverSeen,   \* Keys & secrets the server has ever had unencrypted access to
          userSeen,     \* Keys & secrets each user has ever had unencrypted access to
          pcReplaceKey,

          \* Server-side state:
          data,         \* User data (project content) encrypted with dataKey
          key,          \* dataKey encrypted with keyKey
          secret,       \* secret (unencrypted)
          secretE,      \* secret encrypted with keyKey
          oldSecrets    \* Previously used secrets (unencrypted)

vars == << serverSeen, userSeen, pcReplaceKey, data, key, secret, secretE, oldSecrets >>

UsedSecrets        == oldSecrets \union {secret}
UsersWithoutKeyKey == {u \in Users : key.key \notin userSeen[u]}
UsersWithKeyKey    == {u \in Users : key.key \in userSeen[u]}

-----------------------------------------------------------------------------------------------------------------------

TypeInvariants ==
  /\ serverSeen   \in SUBSET (Keys \union Secrets)
  /\ userSeen     \in [Users -> SUBSET (Keys \union Secrets)]
  /\ pcReplaceKey \in [Users -> Secrets \union {FALSE}]
  /\ data         \in [encrypted: {Data},  key: Keys] \* This is a blob which if decrypted with .key, would produce .encrypted
  /\ key          \in [encrypted: Keys,    key: Keys] \* This is a blob which if decrypted with .key, would produce .encrypted
  /\ secretE      \in [encrypted: Secrets, key: Keys]
  /\ secret       \in Secrets
  /\ oldSecrets   \in SUBSET Secrets

KeyKeyUnlocksDataKey ==
  key.encrypted = data.key

ValueInvariants ==
  /\ secretE.key = key.key      \* secretE encrypted by keyKey
  /\ secretE.encrypted = secret \* secret & secretE are the same secret
  \* debug
\*  /\ PrintT([serverSeen |-> serverSeen, userSeen |-> userSeen, pcReplaceKey |-> pcReplaceKey, data |-> data, key |-> key, secret |-> secret, secretE |-> secretE, oldSecrets |-> oldSecrets])

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
    /\ data         = [encrypted |-> Data,    key |-> dataKey]
    /\ key          = [encrypted |-> dataKey, key |-> keyKey]
    /\ secretE      = [encrypted |-> secret,  key |-> keyKey]
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
        /\ secretE'      = [encrypted |-> secret2,  key |-> keyKey2]
        /\ key'          = [encrypted |-> dataKey2, key |-> keyKey2]
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

\* The only TLA+ operator that can produce a non-symmetric expression when applied to a symmetric expression is CHOOSE
\* MCSymmetry == nope

Spec == Init /\ [][Next]_<<vars>>

========================================================================================================================
