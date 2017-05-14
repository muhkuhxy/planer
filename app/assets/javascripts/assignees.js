'use strict';

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

   $$('.remove').forEach(el => el.addEventListener('click', ev => {
      findParent(ev.target, 'helper').remove();
   }));

   $('#save').addEventListener('click', ev => {
      req('PUT', ev.target.dataset.target, serializeHelpers()).then(response => console.log(response));
   });
}

function findParent(elem, cls) {
   while (elem.parentNode) {
      let parent = elem.parentNode;
      if (hasClass(parent, cls)) {
         return parent;
      } else {
         elem = parent;
      }
   }
}

function hasClass(elem, cls) {
   let clsList = elem.classList;
   return clsList && clsList.contains(cls);
}

function serializeHelpers() {
   const services = ['sicherheit', 'mikro', 'tonanlage'];
   const volunteers = $$('.helper').map(row => {
      const myServices = $$('td input:checked', row).map(checkbox => checkbox.dataset.service);
      return [
         $('td', row).textContent.trim(),
         myServices.map(_ => services.indexOf(_)),
         $('.email', row).textContent
      ];
   });
   return [services, volunteers];
}
