// app.js
const express = require("express")

const app = express()
app.use(express.static('public'))
app.use('/scripts/jose', express.static(__dirname + '/node_modules/jose/dist/browser/'));
app.use('/scripts/axios', express.static(__dirname + '/node_modules/axios/dist/'));
app.use('/scripts/oidc-client-ts', express.static(__dirname + '/node_modules/oidc-client-ts/dist/browser/'));

module.exports = app
