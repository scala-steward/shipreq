import { localiseDate } from "../utils/locale"
import React from "react"

type Props = {
  date: string
  format?: string
}

export default function(p: Props) {
  const d = localiseDate(p.date)
  return <time dateTime={d.toISOString()}>{d.format("LL")}</time>
}
