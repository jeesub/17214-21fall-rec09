const express = require('express')
const app = express()

app.get('/', function (req, res) {
  setTimeout(() => {  res.send("World!"); }, 500);
})

app.listen(3003)

