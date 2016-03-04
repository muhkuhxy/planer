import {$, $$, req} from './common.js';

window.app = (window.app || {});
window.app.assignees = {
   init: init
};

function init() {
   $('.add-helper button').addEventListener('click', ev => {
      const template = $('template');
      const clone = document.importNode(template.content, true);
      $('.helpers tbody').appendChild(clone);
      const input = $('.add-helper input');
      $('.helpers tbody tr:last-child td').textContent = input.value;
      input.value = '';
   });

   $('#save').addEventListener('click', ev => {
      req('PUT', ev.target.dataset.target, serializeHelpers()).then(response => console.log(response));
   });
}

function serializeHelpers() {
   const services = new Set();
   const verbose = $$('.helper').map(row => {
      const myServices = $$('td input:checked', row).map(checkbox => checkbox.dataset.service);
      myServices.forEach(s => services.add(s));
      return {
         name: $('td', row).textContent.trim(),
         services: myServices
      };
   });
   const servicesArray = Array.from(services);
   return [servicesArray,verbose.map(_ => [_.name, _.services.map(s => servicesArray.indexOf(s))])];
}
