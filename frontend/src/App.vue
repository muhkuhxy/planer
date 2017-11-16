<template>
  <div id="app">
    <app-nav></app-nav>

    <div class="container" id="wrapper">

      <header class="row" v-if="plan">
        <div class="col-xs-12">
          <h2>{{ plan.name }}</h2>
        </div>
      </header>

      <smt-table :plan="plan" :current="current">
      </smt-table>

      <smt-placeholder :assignments="assignments" v-if="current" :date="current.date">
      </smt-placeholder>

      <smt-assignees :current="current" :assignees="assignees">
      </smt-assignees>

      <div class="save">
        <button class="btn btn-primary" type="button" @click="save" :disabled="this.saveState === 'working'">Speichern</button>
        <status-indicator :state="saveState" />
          <a class="btn btn-default" href='mailto:@assignees.map(_.email).flatten.mkString(",")'>E-Mail</a>
      </div>

    </div>
  </div>
</template>

<script>
import {$, bus} from './common'
import SmtTable from './components/SmtTable'
import SmtPlaceholder from './components/SmtPlaceholder'
import StatusIndicator from './components/StatusIndicator'
import SmtAssignees from './components/SmtAssignees'
import moment from 'moment'
import service from './plan.service'

export default {
  name: 'app',
  components: {
    SmtTable,
    SmtPlaceholder,
    SmtAssignees,
    StatusIndicator
  },
  data: function () {
    return {
      plan: null,
      assignments: {
        sicherheit: [undefined, undefined],
        mikro: [undefined, undefined],
        tonanlage: [undefined]
      },
      saveState: '',
      current: null,
      assignees: []
    }
  },
  created: function () {
    service.getPlan(19).then(p => {
      this.plan = p
      this.current = p.parts[0]
    }, err => console.log('failed to load plan', err))
    service.getAssignees().then(as => {
      this.assignees = as
    })
    bus.$on('change-serviceweek', dayPlan => {
      const date = moment(dayPlan.date)
      let dow = date.day() === 2 ? 5 : 2
      dayPlan.date = date.day(dow).format('YYYY-MM-DD')
    })
    bus.$on('selected', dayPlan => {
      this.assignments = dayPlan.assignments
      this.current = dayPlan
    })
    bus.$on('assigned', (assignee, service) => {
      var current = this.plan.parts.find(p => this.current === p)
      current.assignments[service.dataset.service][service.dataset.index] = $('.name', assignee).textContent
    })
  },
  methods: {
    save: function () {
      this.saveState = 'working'
      service.save(this.plan).then(result => {
        this.saveState = result.status === 200 ? 'success' : 'error'
        console.log(result)
      }, err => {
        this.saveState = 'error'
        console.log(err)
      })
    }
  }
}
</script>

<style lang="scss">
$icon-font-path: '~bootstrap-sass/assets/fonts/bootstrap/';
@import '~bootstrap-sass/assets/stylesheets/bootstrap';
@import "./assets/print";
@import './assets/settings';

.date-range {
   margin-bottom: 1em;
}

.termine {

   tbody td {
      vertical-align: middle !important;
   }

   tbody tr {
      cursor: pointer;
   }

   @media screen {
      max-height: 200px;
      overflow-y: scroll;

      .printing-date {
         display: none;
      }
   }
}

.save {
  padding: 3rem;
  display: flex;
  justify-content: center;

  .btn, div {
    margin-right: .3em;
  }
}
</style>
