import React from "react"
import kebabCase from "lodash/kebabCase"
import { Link } from "gatsby"
import { Node } from "../config/post"

export const pathForPost: (node: Node) => string =
  (node) => {
    const s = node.frontmatter.slug
    return `/post/${s.replace(/^\/+/, '')}`
  }

export const pathForTag: (name: string) => string =
  (name) => `/tag/${kebabCase(name)}`

export const linkToTag: (name: string) => JSX.Element =
  (name) => <Link to={pathForTag(name)}>{name}</Link>

// export const pathForTagIndex =
//   "/tag"

// export const linkToTagIndex =
//   <Link to={pathForTagIndex}>Tags</Link>
