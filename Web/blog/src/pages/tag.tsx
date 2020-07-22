import { Link, graphql } from "gatsby"
import { pathForTag } from "../utils/routes"
import { pathForTagIndex } from "../utils/routes"
import React from "react"
import SEO from "../components/seo"
import sortBy from "lodash/sortBy"

export const pageQuery = graphql`
  query {
    allMdx {
      tags: group(field: frontmatter___tags) {
        name: fieldValue
        totalCount
      }
    }
  }
`

type Props = {
  data: {
    allMdx: {
      tags: [{
        name: string
        totalCount: number
      }]
    }
  }
}

export default function({ data }: Props) {

  const tags = sortBy(data.allMdx.tags, 'name')

  return (
    <div>

      <SEO
        path     = {pathForTagIndex}
        subtitle = "Tags"
      />

      <div>
        <h1>Tags</h1>
        <ul>
          {tags.map(tag => (
            <li key={tag.name}>
              <Link to={pathForTag(tag.name)}>
                {tag.name} ({tag.totalCount})
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
