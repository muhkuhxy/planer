<script>
import service from '../api/plan.service'
import {req} from '../lib/common'
import {handleUnauthorized} from '../lib/appHelpers'
import moment from 'moment'
import Datepicker from 'vue-datepicker-local'
import Spinner from 'vue-simple-spinner'

const monthNames = ['Januar', 'Februar', 'März', 'April', 'Mai', 'Juni', 'Juli', 'August', 'Septebmer', 'Oktober', 'November', 'Dezember']

const dateLocal = {
  dow: 1,
  monthsHead: monthNames,
  months: monthNames
}

export default {
  data () {
    return {
      plans: [],
      loading: false,
      from: null,
      to: null,
      dateLocal,
      range: []
    }
  },
  created () {
    this.load()
  },
  computed: {
    validRange () {
      return this.range[0] &&
        this.range[1] &&
        moment(this.range[0]).isBefore(this.range[1], 'day')
    }
  },
  watch: {
    '$route': 'load'
  },
  methods: {
    async load () {
      try {
        this.loading = true
        this.plans = await service.list().catch(handleUnauthorized.bind(this))
      } finally {
        this.loading = false
      }
    },
    remove (plan) {
      const index = this.plans.indexOf(plan)
      this.plans.splice(index, 1)
      req.delete('/api/plan/' + plan.id).then(null, err => {
        console.log('couldn\'t delete plan', plan, err)
        this.plans.splice(index, 0, plan)
      })
    },
    async create () {
      await req.post('/api/plan', {
        from: moment(this.range[0]).format('YYYY-MM-DD'),
        to: moment(this.range[1]).format('YYYY-MM-DD')
      })
      this.load()
    }
  },
  components: {
    Datepicker,
    Spinner
  }
}
</script>

<template>
  <div class="container">

    <header class="row">
      <div class="col-xs-12">
        <h2>Pläne</h2>
      </div>
    </header>

    <Spinner v-if="loading"></Spinner>
    <table class="table table-condensed" v-else>
      <thead>
        <tr>
          <th>Zeitraum</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="plan in plans">
          <td>{{ plan.name }}</td>
          <td class="text-right">
            <router-link class="btn btn-xs btn-default" :to="{ name: 'plan', params: { id: plan.id } }">
              <i class="glyphicon glyphicon-pencil"></i>
            </router-link>
            <button class="btn btn-xs btn-danger" @click="remove(plan)">
              <i class="glyphicon glyphicon-trash"></i>
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <div class="row date-range">
      <div class="col-xs-12">
        <h2>Neuer Plan</h2>
        <Datepicker :local="dateLocal" format="DD.MM.YYYY" rangeSeparator="-" v-model="range"></Datepicker>
        <button @click="create" class="btn create btn-primary" :disabled="!validRange">Anlegen</button>
      </div>
    </div>

    <div class="row assignees">
      <div class="col-xs-12">
        <h2>Helfer</h2>
        <router-link to="/assignees">Helfer pflegen</router-link>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.date-range {
   margin-bottom: 1rem;
 }
.assignees {
   margin-bottom: 3rem;
}
.btn.create {
  margin-top: 1rem;
  display: block;
}
</style>
<style lang="scss">
@import '../assets/style';
</style>
