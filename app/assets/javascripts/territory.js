import {$, $$, parseDate, req} from './common.js';

window.app = (window.app || {});
window.app.territory = {
   init: init
};

function init() {
   $$('.streets').forEach(el => el.addEventListener('click', ev => ev.target.classList.toggle('expand')))

   // TODO FIXME
   $$('.log table tbody tr').reduce((acc, val) => {
      console.log(val)
      if (acc)
         val.classList.add('iteration')
       return val.classList.contains('available') ? !acc : acc
   }, false)
}

