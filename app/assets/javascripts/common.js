
export const $ = (q, p = document) => p.querySelector(q);
export const $$ = (q, p = document) => Array.from(p.querySelectorAll(q));

export function parseDate(date) {
   const validDates = ['DD.MM.YYYY', 'YYYY-MM-DD'].map(f =>
         moment(date, f)).filter(d => d.isValid());
   return validDates.length ? validDates[0] : undefined;
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
      const json = JSON.stringify(data);
      if(json.length > 1100) {
         console.log('request may fail due to too much data!');
      }
      req.send(json);
   });

}

