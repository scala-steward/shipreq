import React from "react";
import { graphql } from "gatsby";
import { MDXProvider } from "@mdx-js/react";
import { MDXRenderer } from "gatsby-plugin-mdx";
import { Link } from "gatsby";

const componentsUsed = { Link };

export default function PageTemplate({ data: { mdx } }) {
  return (
    <div>
      <h1>{mdx.frontmatter.title}</h1>
      <MDXProvider components={componentsUsed}>
        <MDXRenderer>{mdx.body}</MDXRenderer>
      </MDXProvider>
    </div>
  )
};

export const pageQuery = graphql`
  query BlogPostQuery($id: String) {
    mdx(id: { eq: $id }) {
      id
      body
      frontmatter {
        title
      }
    }
  }
`;
