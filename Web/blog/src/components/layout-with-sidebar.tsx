import R from "../utils/responsive"
import React from "react"
import Sidebar from "./sidebar"
import styled from "styled-components"

const GridArea = {
  sidebar: 's',
  main: 'm',
}

const Container = styled.div`
  display: grid;
  justify-content: stretch;
  align-content: start;
  margin: 1rem auto;
  padding: 0 1rem;

  ${R.phone`
    grid-template-areas: "${GridArea.sidebar}" "${GridArea.main}";
  `}
  ${R.phoneWide`
    grid-template-areas: "${GridArea.sidebar} ${GridArea.main}";
    gap: 1rem;
  `}
  ${R.tablet`
    grid-template-areas: "${GridArea.sidebar} ${GridArea.main}";
    gap: 3rem;
  `}
  ${R.desktop`
    grid-template-areas: "${GridArea.sidebar} ${GridArea.main}";
    gap: 5rem;
    ${R.maxWidth}
  `}
`

const Cell = {
  sidebar: styled.div`
    grid-area: ${GridArea.sidebar}
  `,

  main: styled.div`
    grid-area: ${GridArea.main}
  `,
}

type Props = {
  children: React.ReactNode
}

export default function(p: Props): JSX.Element {
  return (
    <Container>
      <Cell.sidebar><Sidebar /></Cell.sidebar>
      <Cell.main>{p.children}</Cell.main>
    </Container>
  )
}
