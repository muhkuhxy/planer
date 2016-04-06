import {$, $$, parseDate, req} from './common.js';

window.app = (window.app || {});
window.app.territory = {
   init: init
};

function init() {
   const issueClicked = Rx.Observable.fromEvent(document.querySelectorAll('.issue'), 'click')
      .pluck('target');
   const friendConfirmed = Rx.Observable.fromEvent(document.querySelectorAll('.friend-selection .ok'), 'click')
      .pluck('target')
      .map(button => {
         return req('POST', '/bla', button.previousElementSibling.value)
            .then(x => console.log(x), e => console.log(e));
      }).publish();
   friendConfirmed.connect();
   issueClicked.subscribe(v => {
      console.log('value: %s', v);
      enableFriendSelection(v);
   }, e => console.log('error: %s', e), () => console.log('done'));

   const names = $$('#friends option').map(f => f.value);

   function enableFriendSelection(button) {
      const selection = button.nextElementSibling;
      if(selection.classList.contains('active')) {
         return;
      }
      console.log('enabling',  button)
      selection.classList.add('active');
      const list = $('.list-group', selection);
      const input = $('input', selection);
      const done = issueClicked.merge(friendConfirmed).first();
      done.subscribe(() => selection.classList.remove('active'));
      Rx.Observable.fromEvent(input, 'keyup')
         .pluck('target', 'value')
         .distinctUntilChanged()
         .takeUntil(done)
         .subscribe(search => {
            $$('.list-group-item', list).forEach(li => li.remove());
            if(search.length === 0) {
               return;
            }
            const name = search.toLowerCase();
            console.log(name);
            names.filter(n => n.toLowerCase().contains(name)).forEach(n => {
               const li = document.createElement('li');
               li.setAttribute('class', 'list-group-item');
               li.addEventListener('click', ev => {
                  console.log(n, 'clicked')
                  input.value = n;
               });
               li.textContent = n;
               list.appendChild(li);
            });
         });
   }

}

