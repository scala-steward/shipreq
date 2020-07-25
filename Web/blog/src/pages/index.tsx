import Layout from "../layouts/regular"
import { Link, graphql } from "gatsby"
import React from "react"

export const pageQuery = graphql`
  query {
    allMdx(sort: { fields: [frontmatter___date], order: DESC }) {
      edges {
        node {
          id
          excerpt
          frontmatter {
            title
          }
          fields {
            path
          }
        }
      }
    }
  }
`

type Query = {
  data: {
    allMdx: {
      edges: [{
        node: {
          id: string
          excerpt: string
          frontmatter: {
            title: string
          }
          fields: {
            path: string
          }
        }
      }]
    }
  }
}

export default function({ data }: Query) {
  const { edges: posts } = data.allMdx

  return (
    <Layout seo={{}}>

      <ul>
        {posts.map(({ node: post }) => (
          <li key={post.id}>
            <Link to={post.fields.path}>
              <h2>{post.frontmatter.title}</h2>
            </Link>
            <p>{post.excerpt}</p>
          </li>
        ))}
      </ul>

    </Layout>
  )
}
