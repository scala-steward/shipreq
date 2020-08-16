import { Link } from "gatsby"
import { Node as Post } from "../config/post"
import { pathForPost } from "../utils/routes"
import Date from "./date"
import R from "../utils/responsive"
import React from "react"
import styled from "styled-components"
import TagList from "./tag-list"

type Props = {
  posts: Array<Post>
}

const Item = styled.div`
  margin-bottom: 3.6rem;
  ${R.phoneWide`
    margin-bottom: 2.8rem;
  `}
`

const Header = styled.h1`
  margin-bottom: 0;
  a:not(:hover) { color: #000; }
`

const Attributes = styled.div`
  font-size: 16px;
  margin-top: .4em;
  margin-bottom: .4em;
`

const DateStyle = styled.span`
  color: #888;
`

const AttributeSeparatorStyle = styled.span`
  color: #ddd;
  margin: 0 1.7ex;
`

const Desc = styled.p`
  color: #3a3a3a;
  margin: 0;
`

const AttrSep = (<AttributeSeparatorStyle>|</AttributeSeparatorStyle>)

function renderPost(post: Post) {

  const tags: Array<string> =
    post.frontmatter.tags

  return (
    <Item key={post.id}>

      <Header>
        <Link to={pathForPost(post)}>{post.frontmatter.title}</Link>
      </Header>

      <Attributes>
        <DateStyle><Date date={post.frontmatter.date} /></DateStyle>
        {AttrSep}
        <TagList tags={tags} separator={AttrSep} style={{opacity: 0.7}} />
      </Attributes>

      <Desc>
        {post.frontmatter.desc}
      </Desc>

    </Item>
  )
}

export default ({ posts } : Props) => {

  return (
    <div>
      {posts.map(renderPost)}
    </div>
  )
}
