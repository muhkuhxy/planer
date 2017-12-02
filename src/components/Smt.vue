<template>
  <div class="container" id="wrapper">

    <header class="row">
      <div class="col-xs-12">
        <h2>{{ title }}</h2>
      </div>
    </header>

    <smt-table>
    </smt-table>

    <smt-placeholder>
    </smt-placeholder>

    <smt-assignees :assignees="assignees">
    </smt-assignees>

    <div class="save">
      <button class="btn btn-primary" type="button" @click="save" :disabled="this.saveState === 'working'">Speichern</button>
      <status-indicator :state="saveState"></status-indicator>
      <a class="btn btn-default" href='mailto:@assignees.map(_.email).flatten.mkString(",")'>E-Mail</a>
    </div>

  </div>
</template>

<script>
import SmtTable from './SmtTable'
import SmtPlaceholder from './SmtPlaceholder'
import StatusIndicator from './StatusIndicator'
import SmtAssignees from './SmtAssignees'
import service from '../api/plan.service'
import {handleUnauthorized} from '../lib/appHelpers'
import {mapState} from 'vuex'
import moment from 'moment'

export default {
  components: {
    SmtTable,
    SmtPlaceholder,
    SmtAssignees,
    StatusIndicator
  },
  data: function () {
    return {
      saveState: '',
      assignees: []
    }
  },
  props: ['id'],
  computed: mapState(['title', 'assignments']),
  created: function () {
    Promise.all([
      service.getPlan(this.id),
      service.getAssignees()
    ]).then(res => {
      let [p, as] = res
      this.$store.commit('load', p)
      this.assignees = as
    }, handleUnauthorized.bind(this))
  },
  methods: {
    save: function () {
      this.saveState = 'working'
      service.save(this.serialize()).then(result => {
        this.saveState = result.status === 200 ? 'success' : 'error'
      }, err => {
        this.saveState = 'error'
        console.log(err)
      })
    },
    serialize () {
      const names = this.assignees.map(_ => _.name)
      const lookup = names.reduce((map, name, index) => { map[name] = index; return map }, {})
      const performLookup = n => lookup[n]
      return [
        parseInt(this.id),
        this.title,
        [
          names,
          this.assignments.map(a => [
            a.id,
            moment(a.date).format('YYYY-MM-DD'),
            [
              a.sicherheit.map(performLookup),
              a.mikro.map(performLookup),
              a.tonanlage.map(performLookup)
            ],
            a.unavailable.map(performLookup)
          ])
        ]
      ]
    }
  }
}
</script>

<style lang="scss">
$icon-font-path: '~bootstrap-sass/assets/fonts/bootstrap/';
@import '~bootstrap-sass/assets/stylesheets/bootstrap';
@import "../assets/print";
@import '../assets/settings';

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
