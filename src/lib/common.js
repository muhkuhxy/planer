'use strict'
import moment from 'moment'

export const $ = (q, p = document) => p.querySelector(q)
export const $$ = (q, p = document) => Array.from(p.querySelectorAll(q))

export function parseDate (date) {
  const [maybeValid] = ['DD.MM.YYYY', 'YYYY-MM-DD'].map(f =>
    moment(date, f)).filter(d => d.isValid())
  return maybeValid
}

// eslint-disable-next-line no-extend-native
Promise && (Promise.prototype.finally = function (block) {
  this.then(x => block(x), x => block(x))
})

export function req (method, url, data) {
  /* eslint-disable-next-line no-new */
  return new Promise((resolve, reject) => {
    /* eslint-disable-next-line no-new */
    let req = new XMLHttpRequest()
    if (resolve) {
      req.addEventListener('load', function () {
        if (this.status >= 400) {
          reject(new Error(this.status))
        }
        resolve(this)
      })
    }
    if (reject) {
      req.addEventListener('error', function () {
        reject(new Error(this))
      })
    }
    req.open(method, url)
    req.setRequestHeader('content-type', 'application/json')
    req.setRequestHeader('csrf-token', 'nocheck')
    let json
    if (data) {
      json = JSON.stringify(data)
      if (json && json.length > 1100) {
        console.log('request may fail due to too much data!')
      }
    }
    req.send(json)
  })
}

for (let verb of ['GET', 'PUT', 'POST', 'DELETE']) {
  req[verb.toLowerCase()] = function (url, data) {
    return req(verb, url, data)
  }
}
