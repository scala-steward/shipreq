import React from "react"
import { Link as GatsbyLink } from "gatsby"
import styled from "styled-components"
import ShipreqBanner from "./shipreq-banner"

const Link = styled(GatsbyLink)`
  display: inline-block;
  width: 100%;
  max-width: 500px;
`

const Title = styled.h1`
  margin: 0;
  text-align: right;
  font-weight: bold;
  color: #00488c;
  font-size: 250%;
`

export default function(): JSX.Element {
  return (
    <Link to="/">
      <ShipreqBanner width="100%" />
      <Title>Blog</Title>
    </Link>
  )
}
