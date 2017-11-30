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
      <status-indicator :state="saveState" />
        <a class="btn btn-default" href='mailto:@assignees.map(_.email).flatten.mkString(",")'>E-Mail</a>
    </div>

  </div>
</template>

<script>
import SmtTable from './SmtTable'
import SmtPlaceholder from './SmtPlaceholder'
import StatusIndicator from './StatusIndicator'
import SmtAssignees from './SmtAssignees'
import service from '../plan.service'

export default {
  components: {
    SmtTable,
    SmtPlaceholder,
    SmtAssignees,
    StatusIndicator
  },
  data: function () {
    return {
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
  computed: {
    title () {
      return this.$store.state.title
    }
  },
  created: function () {
    service.getPlan(this.$route.params.id).then(p => {
      this.$store.commit('load', p)
    }, err => console.log('failed to load plan', err))
    service.getAssignees().then(as => {
      this.assignees = as
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
@import "../assets/print";
@import '../assets/settings';

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
