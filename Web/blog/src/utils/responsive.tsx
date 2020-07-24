import media from "styled-media-query"

export default {

  phone    : media.lessThan("small"),
  phoneWide: media.between("small", "medium"),
  tablet   : media.between("medium", "large"),
  desktop  : media.greaterThan("large"),

  maxWidth : "max-width: 1024px;"
}
