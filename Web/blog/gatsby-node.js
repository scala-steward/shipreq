const post = require("./src/config/post")

exports.createPages = async input => {
  await post.createPages(input)
}
