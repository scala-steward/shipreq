["#", "Step", "Remote", "Tabs", "Workers", "Network"],
["=", "====", "======", "====", "=======", "======="],
(
  .

  # Condense drafts into a "worker:time:prov" format like "1.5:{1.3}"
  | (.. | select(.prov?)) |= ("\(.worker).\(.time)\(.prov | with_entries(select(.value>0)))" | gsub(":";".") | gsub("w";"") | gsub("{}";""))

  | .[]
  | [
    .no,
    .name,
    (.state.remote? // "-" | tostring),
    (.state.tabs? | with_entries(.value |= (.draft?.get? // .drafts // []))? // "-" | tostring),
    (.state.workers? | with_entries(.value |= (.drafts? // "-"))? // "-" | tostring),
    (
      .state.network?
      | ([ .[] | select(.drafts?) | "\(.from)→\(.to):\(.drafts)" ] | sort)?
      // [] | tostring)
  ]
)
| @tsv
| gsub("[\"\\\\]"; "")
| gsub(","; ", ")
