import { Node } from "../config/post"
import { pathForPost } from "../utils/routes"
import { Props as IconButtonProps } from "./icon-button"
import IconButtons from "./icon-buttons"
import React from "react"
import site from "../config/site"

type Props = {
  post: Node
}

function encodeQuery(q: {[k: string]: string}): string {
  const comps: Array<string> = []
  for (let k in q) {
    const v = q[k]
    comps.push(`${k}=${encodeURIComponent(v)}`)
  }
  return "?" + comps.join("&")
}

export default ({ post }: Props) => {
  const p   = post.frontmatter
  const url = pathForPost(post)

  const linkedInParams = {
    url,
    mini: "true",
    title: p.title.substr(0, 200),
    summary: p.desc.substr(0, 256),
    source: site.title,
  }

  const linkedInUrl = "https://www.linkedin.com/shareArticle" + encodeQuery(linkedInParams)

  const buttons: Array<IconButtonProps> = []
  if (p.twitter) buttons.push({ icon: "twitter",  href: p.twitter   })
  if (p.reddit)  buttons.push({ icon: "reddit",   href: p.reddit    })
                 buttons.push({ icon: "linkedIn", href: linkedInUrl })

  return (
    <>
      <div>Share / Discuss: </div>
      <IconButtons buttons={buttons} />
    </>
  )
}
