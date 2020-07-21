import React from "react"
import styled from "styled-components"
import { graphql } from "gatsby"
import SEO from "../components/seo"
import "./404.css"

const Container = styled.div`
  height:66.67vh; width:100vw; display:flex;`

export default function({ data }: Query) {
  const md = data.site.siteMetadata

  return (
    <Container>
      <SEO
        article = {false}
        desc    = {md.description}
        path    = {null}
        title   = {`404 | ${md.title}`}
      />
      <main>
        <div id="a">404</div>
        <div id="b">Page not found.</div>
        <div id="c"><a href="/">Let's go home...</a></div>
      </main>
    </Container>
  )
}

type Query = {
  data: {
    site: {
      siteMetadata: {
        description: string,
        title: string,
      }
    }
  }
}

export const pageQuery = graphql`
  query {
    site {
      siteMetadata {
        description
        title
      }
    }
  }
`
