["#", "Step", "Target", "Remote", "Tabs", "Workers", "Network", "Browser"],
["=", "====", "======", "======", "====", "=======", "=======", "======="],
(
  .

  # Condense drafts into a "worker:time:prov" format like "1.5:{1.3}"
  | (.. | select(.prov?)) |= (
      (.prov | with_entries(select(.value>0))) as $prov
      | (if .tombstone? then "d" else "" end) as $tomb
      | "\(.worker).\(.time)\($prov)\($tomb)"
      | gsub(":";".") | gsub("w";"") | gsub("{}";"")
    )

  | .[]
  | [
    .no,
    .name,
    (
      (.state.target.drafts? // "-" | tostring) as $drafts
      | ((.state.target.pending | [.[] | "\(.tab):\(.editCount)"] | join(","))? // "") as $pending
      | if $pending == "" then $drafts else "\($drafts)+{\($pending)}" end
    ),
    (.state.remote.drafts? // "-" | tostring),
    (.state.tabs?
      | with_entries(
          if .value.status == "-" then
            .value |= "-"
          elif .value.status == "clean" then
            .value |= (if .tombstones == [] then "-" else "-\(.tombstones)" end)
          else
            .value |= (
              (if (.editRev? > 0) then "+" else "" end) as $dirty
              | (if .aborted? then "A" else "" end) as $aborted
              | (if .editRevAck? != .editRevSent? then "?" else "" end) as $pending
              | "\(.drafts? // [])\($dirty)\($aborted)\($pending)"
            )
          end
        )?
      // "-"
      | tostring
    ),
    (.state.workers?
      | with_entries(.value |= (.drafts? // "-"))?
      // "-"
      | tostring
    ),
    (
      .state
      | if .network? then (
          [ .network[] | select(.drafts?) ] as $n2
          | ($n2 == [] and .network != []) as $acksOnly
          | if $acksOnly then
              "[…]"
            else (
              [ $n2[]
                | select(.drafts?)
                | "\(.type | sub(":.*";""))(\(.from)→\(.to)):\(.drafts)\(if .edit.get? then "+" else "" end)"
              ]
              | sort
              | tostring
              | gsub("Remote"; "R")
            )
            end
        ) else
          "-"
        end
    ),
    (.state.browsers?
      | with_entries(.value |= (with_entries(.value |= (.get? // "-"))))?
      // "-"
      | tostring
    )
  ]
)
| @tsv
| gsub("[\"\\\\]"; "")
| gsub(","; ", ")
