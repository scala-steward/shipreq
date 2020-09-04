const express = require('express')
const { createProxyMiddleware } = require('http-proxy-middleware')

const host = '0.0.0.0'
const port = 6666
const app  = express()

const target = process.env.TARGET
if (typeof target !== 'string')
  throw `TARGET env var not specified.`

const onProxyReq = function (proxyReq, req, res) {
  const now = new Date()
  console.log(`[${now.toLocaleTimeString()}] Proxying ${proxyReq.path}`)
}

const proxyOptions = {
  target: `${target}/s`,
  changeOrigin: false,
  onProxyReq,
}
app.use('/', createProxyMiddleware(proxyOptions))

// Start server
app.listen(port, host, () => {
  console.log(`Fake CDN listening at http://${host}:${port}`)
  console.log('')
})
