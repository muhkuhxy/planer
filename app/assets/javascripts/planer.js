import {$, $$, parseDate, req} from './common.js';

const dienste = ['sicherheit', 'mikro', 'tonanlage'];

window.app = (window.app || {});
window.app.planer = {
   init: init
};

moment.locale('de', {
  weekdays: ['Sonntag', 'Montag', 'Dienstag', 'Mittwoch', 'Donnerstag', 'Freitag', 'Samstag']
})
moment.locale('de')

function init() {
  let bus = new Vue()

  Vue.component('status-indicator', {
    props: ['state'],
    template: `
      <div class="working" v-if="state == 'working'">
         <span class="glyphicon glyphicon-repeat"></span>
      </div>
      <div class="save-ok" v-else-if="state == 'success'">
         <span class="glyphicon glyphicon-ok-circle"></span>
      </div>
      <div class="error" v-else-if="state == 'error'">
         <span class="glyphicon glyphicon-remove-circle"></span>
      </div>
    `
  })

  Vue.component('smt-table', {
    props: ['plan'],
    data: function() {
      return {
        today: moment().format("DD.MM.YYYY")
      }
    },
    methods: {
      select: function(a) {
        bus.$emit('selected', a)
        console.log('emitting ', a)
      },
    },
    template: `
      <div class="row termine" v-if="plan">
         <table class="table table-condensed">
            <thead>
               <tr>
                  <th>Datum</th>
                  <th class="serviceweek">Dienstwoche</th>
                  <th>Sicherheit</th>
                  <th>Mikro</th>
                  <th>Tonanlage</th>
               </tr>
            </thead>
            <tbody>
              <template v-for="part in plan.parts">
                 <rowOne :dayPlan="part" @selected="select">
                 </rowOne>
                 <tr @click="select(part)">
                    <td class="sicherheit">{{ part.assignments.sicherheit[1] }}</td>
                    <td class="mikro">{{ part.assignments.mikro[1] }}</td>
                 </tr>
              </template>
            </tbody>
         </table>

         <div class="printing-date">{{ today }}</div>
      </div>
    `,
    components: {
      'rowOne': {
        props: ['dayPlan'],
        computed: {
          dayOfWeek: function() {
            return this.date.day()
          },
          date: function() {
            return moment(this.dayPlan.date)
          },
          formattedDate: function() {
            return this.date.format('dddd, DD.MM.YYYY')
          }
        },
        methods: {
          change: function() {
            bus.$emit('change-serviceweek', this.date)
          },
          select: function() {
            console.log('emit deep')
            bus.$emit('selected', this.dayPlan)
          }
        },
        template: `
             <tr @click="select">
                <td class="datum" rowspan="2">{{ formattedDate }}</td>
                <td class="serviceweek" rowspan="2">
                  <input v-if="dayOfWeek !== 0" type="checkbox" :checked="dayOfWeek === 2" @change.stop.prevent="change" />
                </td>
                <td class="sicherheit">{{ dayPlan.assignments.sicherheit[0] }}</td>
                <td class="mikro">{{ dayPlan.assignments.mikro[0] }}</td>
                <td class="tonanlage" rowspan="2">{{ dayPlan.assignments.tonanlage[0] }}</td>
             </tr>
        `
      }
    }
  })

  Vue.component('smt-placeholder', {
    props: ['assignments', 'date'],
    computed: {
      formatted: function() {
        return moment(this.date).format('DD.MM.YYYY')
      }
    },
    template: `
      <div class="row planung" v-if="date">
         <h2 class="col-xs-12">Plan für <span>{{ formatted }}</span></h2>
         <ph name="sicherheit" :assignment="assignments.sicherheit"></ph>
         <ph name="mikro" :assignment="assignments.mikro"></ph>
         <ph name="tonanlage" :assignment="assignments.tonanlage"></ph>
      </div>
    `,
    components: {
      ph: {
        props: ['name', 'assignment'],
        methods: {
          remove: function() {
          }
        },
        template: `
         <div class="col-md-4" :class="name">
            <h3>{{ name }}</h3>
            <div class="platzhalter" v-for="ass in assignment">
               <div class="remove" @click="remove()">
                  <span class="glyphicon glyphicon-remove"></span>
               </div>
               <div class="slot"></div>
            </div>
         </div>
        `
      }
    }
  })

  new Vue({
    el: '#wrapper',
    data: {
      plan: null,
      date: null,
      assignments: {
        sicherheit: [undefined, undefined],
        mikro: [undefined, undefined],
        tonanlage: [undefined]
      },
      saveState: ''
    },
    created: function() {
      let url = $('#api').dataset.url
      console.log('created ', url)
      req.get(url).then(response => {
        console.log(response)
        let plan = JSON.parse(response.responseText)
        plan.parts.forEach(p => {
          let {sicherheit = [], mikro = [], tonanlage = []} = p.assignments
          sicherheit.length = 2
          mikro.length = 2
          tonanlage.length = 1
          p.assignments = {
            sicherheit: sicherheit,
            mikro: mikro,
            tonanlage: tonanlage
          }
        })
        this.plan = plan
      })
      bus.$on('change-serviceweek', date => {
        let dow = date.day() === 2 ? 5 : 2;
        let formatted = date.format('YYYY-MM-DD')
        this.plan.parts.find(p => p.date === formatted).date = date.day(dow).format('YYYY-MM-DD')
      })
      bus.$on('selected', dayPlan => {
        console.log('received', dayPlan)
        this.assignments = dayPlan.assignments
        this.date = dayPlan.date
      })
    },
    methods: {
      save: function() {
        this.saveState = 'working'
        req.put(this.url, this.plan).then(result => {
          this.saveState = result.status === 200 ? 'success' : 'error'
          console.log(result)
        }, err => {
          this.saveState = 'error'
          console.log(result)
        })
      }
    }
  })


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
   // saveButton.addEventListener('click', ev => {
   //    $('.working').classList.remove('hide');
   //    $('.save-ok').classList.add('hide');
   //    $('.error').classList.add('hide');
   //       $('.working').classList.add('hide');
   //       if(result.status === 200) {
   //          $('.save-ok').classList.remove('hide');
   //       }
   //       else {
   //          $('.error').classList.remove('hide');
   //       }
   //    }, err => {
   //       $('.working').classList.add('hide');
   //       $('.error').classList.remove('hide');
   //    });
   // });

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

function getName(candidate) {
   return $('.name', candidate).textContent;
}

function serializePlan() {
   const names = $$('.kandidaten li > div .name').map(_ => _.textContent);
   const lookup = _ => names.indexOf(_);
   let parts = $$('.termine tbody tr:nth-child(odd)').map(row => {
      let assignments = assignmentsFor(row);
      let {unavailable} = assignments;
      const serviceweekCb = $('.serviceweek input', row)
      delete assignments.unavailable;
      return [
         parseDate($('td.datum', row).textContent.substr(5)).format('YYYY-MM-DD'),
         dienste.map(s => assignments[s].map(lookup)),
         unavailable.map(lookup),
         serviceweekCb ? serviceweekCb.checked : false
      ]
   });
   const id = parseInt($('.termine table').dataset.id, 10);
   const name = $('header h2').textContent;
   const plan = [ id, name, [names, parts] ];
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
   $('.slot', platzhalter).textContent = getName(bruder);
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
   const name = getName(bruder);
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
      $('[name=unavailable]', b).checked = true;
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
         addToPlan(getName(dragged), e.target);
      }
   });

}
