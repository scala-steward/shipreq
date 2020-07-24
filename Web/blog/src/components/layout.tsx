import { exhaustiveCheck } from "../utils/utils"
import { SEO, Props as SeoProps } from "./seo"
import Analytics from "./analytics"
import LayoutWithoutSidebar from "./layout-without-sidebar"
import LayoutWithSidebar from "./layout-with-sidebar"
import React from "react"

export enum Page {
  Index,
  Post,
  TagIndex,
  TagPosts,
}

export interface Props {
  page    : Page
  seo     : SeoProps
  children: React.ReactNode
}

const withSidebar    = (p: Props) => <LayoutWithSidebar>{p.children}</LayoutWithSidebar>
const withoutSidebar = (p: Props) => <LayoutWithoutSidebar>{p.children}</LayoutWithoutSidebar>

function layout(p: Props): JSX.Element {
  switch (p.page) {
    case Page.Index   : return withSidebar(p)
    case Page.Post    : return withoutSidebar(p)
    case Page.TagIndex: return withSidebar(p)
    case Page.TagPosts: return withSidebar(p)
  }
  exhaustiveCheck(p.page)
}

export function Layout(p: Props): JSX.Element {
  return (<>
    <Analytics />
    <SEO />
    {layout(p)}
  </>)
}
