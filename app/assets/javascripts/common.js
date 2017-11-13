'use strict';

export const $ = (q, p = document) => p.querySelector(q);
export const $$ = (q, p = document) => Array.from(p.querySelectorAll(q));

export function parseDate(date) {
   const [maybeValid] = ['DD.MM.YYYY', 'YYYY-MM-DD'].map(f =>
         moment(date, f)).filter(d => d.isValid());
   return maybeValid;
}

export function req(method, url, data) {
   return new Promise((resolve, reject) => {
      let req = new XMLHttpRequest();
      if(resolve) {
         req.addEventListener('load', function() {
            resolve(this);
         });
      }
      if(reject) {
         req.addEventListener('error', function() {
            reject(this);
         });
      }
      req.open(method, url);
      req.setRequestHeader('content-type', 'application/json');
      let json;
      if (data) {
         json = JSON.stringify(data);
         if(json && json.length > 1100) {
            console.log('request may fail due to too much data!');
         }
      }
      req.send(json);
   });

}

for (let verb of ['GET', 'PUT', 'POST', 'DELETE']) {
  req[verb.toLowerCase()] = function(url, data) {
    return req(verb, url, data);
  }
}

