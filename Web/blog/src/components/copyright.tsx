import R from "../utils/responsive"
import React from "react"
import site from "../config/site"
import styled from "styled-components"

type Props = {
  flattenOnPhones?: boolean
}

const Copyright = styled.div`
  color: #aaa;
  font-size: 75%;
  line-height: 1.5em;
`

const FlattenOnPhones = styled(Copyright)`
  ${R.phoneAny`
    display: flex;
    justify-content: center;
    div:first-child {
      margin-right: 1ex;
    }
  `}
`

export default function(p: Props = {}) {
  return p.flattenOnPhones ?
  (
    <FlattenOnPhones>
      <div>{site.copyright1}</div>
      <div>{site.copyright2}</div>
    </FlattenOnPhones>
  ) : (
    <Copyright>
      <div>{site.copyright1}</div>
      <div>{site.copyright2}</div>
    </Copyright>
  )
}
