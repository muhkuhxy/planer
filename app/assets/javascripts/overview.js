import {$, $$, parseDate, req} from './common.js';
let start, end, createButton;

window.app = (window.app || {});
window.app.overview = {
   init: init
};

function init() {
   const from = $('input[name=from]');
   const to = $('input[name=to]');
   start = parseDate(from.value);
   end = parseDate(to.value);
   from.addEventListener('change', function(event) {
      start = parseDate(event.target.value);
      updateRange(start, end);
   });
   to.addEventListener('change', function(event) {
      end = parseDate(event.target.value);
      updateRange(start, end);
   });
   createButton = $('#create');
   createButton.addEventListener('click', ev => {
      req('POST', createButton.dataset.target, {
         from: start.format('YYYY-MM-DD'),
         to: end.format('YYYY-MM-DD')
      }).then(result => {
         location.href = result.response;
      });
   });

   function updateRange(start, end) {
      createButton.disabled = !(start && end);
   }

   $$('.plan-list .plan-remove').forEach( button => {
      button.addEventListener('click', ev => {
         req('DELETE', button.dataset.target).then(() => {
            button.parentNode.remove();
         })
      });
   });

}

