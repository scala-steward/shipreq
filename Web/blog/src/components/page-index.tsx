import { Link } from "gatsby"
import React from "react"

type Page = {
  name: React.ReactNode
  path: string
}

const pages: Array<Page> =
  [
    { name: "about", path: "/about" },
  ]

export default () => {


  return (
    <nav>
      {pages.map(p => (

        <Link key={p.path} to={p.path}>{p.name}</Link>

      ))}
    </nav>
  )
}
