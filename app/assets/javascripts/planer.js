import {$, $$, parseDate, req} from './common.js';

const dienste = ['sicherheit', 'mikro', 'tonanlage'];

window.app = (window.app || {});
window.app.planer = {
   init: init
};

function init() {
   initDragAndDrop();

   $$('.termine tbody tr:nth-child(odd)').forEach( primaryRow => {
      [primaryRow, primaryRow.nextElementSibling].forEach( row => {
         row.addEventListener('click', ev => {
            selectRow(primaryRow);
         });
      });
   });

   $$('input[name=unavailable]').forEach(cb => {
      cb.addEventListener('change', ev => {
         if(ev.target.checked) {
            ev.target.parentNode.classList.add('disabled');
            unavailable(ev.target.parentNode);
         } else {
            ev.target.parentNode.classList.remove('disabled');
            unavailable(ev.target.parentNode, true);
         }
      });
   });

   let saveButton = $('#save');
   saveButton.addEventListener('click', ev => {
      req('PUT', saveButton.dataset.target, serializePlan()).then(result => {
         if(result.status === 200) {
            $('.save-ok').classList.remove('hide');
         }
      });
   });

    $$('.platzhalter .remove').forEach(el => {
       el.addEventListener('click', () => {
          const slot = el.nextElementSibling;
          const name = slot.textContent.trim();
          if(name) {
             unassign(name);
             slot.textContent = '';
             const platzhalter = slot.parentNode;
             platzhalter.classList.remove('bg-success');
             addToPlan('', platzhalter);
          }
       });
    });
}

function serializePlan() {
   let parts = $$('.termine tbody tr:nth-child(odd)').map(row => {
      let assignments = assignmentsFor(row);
      let {unavailable} = assignments;
      delete assignments.unavailable;
      return {
         date: parseDate($('td.datum', row).textContent.substr(5)).format('YYYY-MM-DD'),
         assignments: assignments,
         unavailable: unavailable
      }
   });
   const id = parseInt($('.termine table').dataset.id, 10);
   const name = $('header h2').textContent;
   const plan = [ id, name, parts ];
   console.log(plan)
   return plan;
}

function findBruderByName(name, brueder = $$('.kandidaten li > div')) {
   return brueder.find(b => b.textContent.indexOf(name) !== -1);
}

function getDataUnavailable(tr) {
   let value = tr.dataset.unavailable;
   const array = value ? value.split(',') : [];
   return new Set(array);
}

function assign(bruder, platzhalter, count = true) {
   platzhalter.classList.add('bg-success');
   bruder.classList.add('assigned');
   $('input[name=unavailable]', bruder).disabled = true;
   if(count) {
      updateCounter(bruder, 1);
      const prev = $('.slot', platzhalter).textContent;
      if(prev) {
         unassign(prev);
      }
   }
   $('.slot', platzhalter).textContent = bruder.firstChild.textContent;
}

function updateCounter(bruder, val) {
   let counter = $('.dienste .counter', bruder);
   counter.textContent = parseInt(counter.textContent, 10) + val;
}

function unassign(name) {
   $$('.kandidaten .assigned').forEach(b => {
      if(b.textContent.indexOf(name) !== -1) {
         b.classList.remove('assigned');
         $('input[name=unavailable]', b).disabled = false;
         updateCounter(b, -1);
      }
   });
}

function addToPlan(name, platzhalter) {
   const parentClasses = platzhalter.parentNode.classList;
   const dienst = dienste.find(d => parentClasses.contains(d));
   let idx = 0;
   if((dienst === 'sicherheit' || dienst === 'mikro') &&
         platzhalter.nextElementSibling === null) {
      idx = 1;
   }
   const row = $$('.termine tbody tr.info');
   const cell = $(`td.${dienst}`, row[idx]);
   cell.textContent = name;
}

function unavailable(bruder, remove) {
   const tr = $('.termine tr.info');
   const name = bruder.firstChild.textContent.trim();
   const set = getDataUnavailable(tr);
   if(remove) {
      set.delete(name);
   }
   else {
      set.add(name);
   }
   tr.setAttribute('data-unavailable', Array.from(set).join(','));
}

function predecessor(element, parentTag) {
   const tagRe = new RegExp(parentTag, 'i');
   let parentNode = element;
   do {
      parentNode = parentNode.parentNode;
   } while(parentNode !== document && !tagRe.test(parentNode.tagName));
   if(!tagRe.test(parentNode.tagName)) {
      return;
   }
   return parentNode;
}

function markCurrentRow(tr) {
   $$('.termine tbody .info').forEach(n =>
         n.classList.remove('info'));
   tr.classList.add('info');
   tr.nextElementSibling.classList.add('info');
}

function resetAssignments() {
   $$('.kandidaten .assigned').forEach(b => {
      b.classList.remove('assigned');
   });
   $$('.kandidaten input[name=unavailable]').forEach(cb => {
      cb.disabled = false;
      cb.checked = false;
      cb.parentNode.classList.remove('disabled');
   });
   $$('.platzhalter').forEach(p => {
      p.classList.remove('bg-success');
      $('.slot', p).textContent = '';
   });
}

function assignmentsFor(row) {
   let uaStored = Array.from(getDataUnavailable(row));
   return {
      sicherheit: [
         $('.sicherheit', row).textContent.trim(),
         $('.sicherheit', row.nextElementSibling).textContent.trim()
      ],
      mikro: [
         $('.mikro', row).textContent.trim(),
         $('.mikro', row.nextElementSibling).textContent.trim()
      ],
      tonanlage: [
         $('.tonanlage', row).textContent.trim()
      ],
      unavailable: uaStored
   };
}

function applyAssignments(assigned) {
   const brueder = $$('.kandidaten li > div');
   assigned.unavailable.forEach(un => {
      const b = findBruderByName(un, brueder);
      b.children[0].checked = true;
      b.classList.add('disabled');
   });
   dienste.forEach(dienst => {
      const platzhalter = $$(`.planung .${dienst} .platzhalter`);
      assigned[dienst].forEach((name, i) => {
         if(!name) { return; }
         const b = findBruderByName(name, brueder);
         const platzhalterRow = platzhalter[i];
         assign(b, platzhalterRow, false);
      });
   });
}

function selectRow(row) {
   markCurrentRow(row);
   $('.kandidaten .blocker').classList.add('hide');
   $('.planung h2 span').textContent = $('tr.info td.datum').textContent;
   resetAssignments();
   const assignments = assignmentsFor(row);
   applyAssignments(assignments);
}

function initDragAndDrop() {
   let dragged;

   document.addEventListener('dragstart', e => {
      dragged = e.target;
      if(e.target.classList.contains('disabled') ||
            e.target.classList.contains('assigned')) {
         e.preventDefault();
         return;
      }
      e.dataTransfer.setData('text/plain', null);
      e.target.style.opacity = .5;
   });

   document.addEventListener('dragend', e => {
      dragged.style.opacity = '';
   });

   function classesOverlap(placeholder) {
      return overlap($$('.dienste span', dragged).map(span => span.className),
            Array.from(placeholder.classList));
   }

   function overlap(s1, s2) {
      const b = new Set(s2);
      return s1.some(a => b.has(a));
   }

   document.addEventListener('dragenter', e => {
      if(e.target.classList &&
         ((e.target.classList.contains('platzhalter') && classesOverlap(e.target.parentNode, dragged)) ||
         (e.target.classList.contains('slot') && classesOverlap(e.target.parentNode.parentNode, dragged)))) {
         e.target.style.backgroundColor = '#aaa';
      } else {
         e.preventDefault();
      }
   }, false);

   document.addEventListener('dragover', e => {
      e.preventDefault();
   });

   document.addEventListener('dragleave', e => {
      if(!e.target || !e.target.style) {
         return;
      }
      e.target.style.backgroundColor = '';
   });

   document.addEventListener('drop', e => {
      e.preventDefault();
      if(e.target.classList.contains('platzhalter') && classesOverlap(e.target.parentNode)) {
         e.target.style.backgroundColor = '';
         assign(dragged, e.target);
         addToPlan(dragged.firstChild.textContent, e.target);
      }
   });

}
