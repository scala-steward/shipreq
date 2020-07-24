import React from "react"
import Banner from "./shipreq-blog-banner"
import styled from "styled-components"
import media from "styled-media-query"

export interface Props {
}

export default function(p: Props) {

  return (
    <aside>
      <Banner />
    </aside>
  )
}
