<script>
import SmtTable from './SmtTable'
import StatusIndicator from './StatusIndicator'
import SmtAssignees from './SmtAssignees'
import service from '../api/plan.service'
import {mapState} from 'vuex'
import moment from 'moment'
import Spinner from 'vue-simple-spinner'

export default {
  components: {
    SmtTable,
    SmtAssignees,
    StatusIndicator,
    Spinner
  },
  data: function () {
    return {
      saveState: '',
      loading: false
    }
  },
  props: ['id'],
  computed: {
    ...mapState(['title', 'assignments', 'assignees', 'services']),
    emails () {
      return this.assignees.map(_ => _.email).filter(_ => _).join(',')
    },
    mailto () {
      return 'mailto:' + this.emails + '?subject=Einteilung Sicherheit, Mikro, Tonanlage vom ' + this.title
    }
  },
  created () {
    this.load()
  },
  watch: {
    '$route': 'load'
  },
  methods: {
    load () {
      this.loading = true
      this.$store.dispatch('loadPlan', this.id)
        .finally(_ => {
          this.loading = false
        })
    },
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
      return {
        id: parseInt(this.id),
        parts: this.assignments.map(a => {
          return {
            id: a.id,
            date: moment(a.date).format('YYYY-MM-DD'),
            assignments: this.services.map(service => {
              return {
                s: service.id,
                shifts: a[service.name].map(a => a && a.id)
              }
            }),
            unavailable: a.unavailable
          }
        })
      }
    }
  }
}
</script>

<template>
  <div class="container">

    <Spinner v-if="loading"></Spinner>
    <div v-else>
      <SmtTable/>
      <SmtAssignees :assignees="assignees"/>
    </div>

    <div class="save">
      <button class="btn btn-primary" type="button" @click="save" :disabled="this.saveState === 'working'">Speichern</button>
      <status-indicator :state="saveState"></status-indicator>
      <a class="btn btn-default" :href="mailto">E-Mail</a>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.save {
  padding: 3rem;
  display: flex;
  justify-content: center;

  .btn, div {
    margin-right: .3em;
  }
}
</style>
