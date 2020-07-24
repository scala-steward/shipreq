import R from "../utils/responsive"
import React from "react"
import styled from "styled-components"
import Banner from "./shipreq-blog-banner"

const Container = styled.div`
  margin: 1rem auto;
  padding: 0 1rem;

  ${R.phone`
  `}
  ${R.phoneWide`
  `}
  ${R.tablet`
  `}
  ${R.desktop`
    ${R.maxWidth}
  `}
`

const BannerContainer = styled.header`
  text-align: center;

  ${R.phone`
    margin-bottom: 2.2rem;
  `}
  ${R.phoneWide`
    margin-bottom: 1rem;
  `}
  ${R.tablet`
    margin-bottom: 1rem;
  `}
  ${R.desktop`
    margin-bottom: 1rem;
  `}
`

type Props = {
  children: React.ReactNode
}

export default function(p: Props): JSX.Element {
  return (
    <Container>
      <BannerContainer>
        <Banner />
      </BannerContainer>
      {p.children}
    </Container>
  )
}
