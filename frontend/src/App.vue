<template>
  <div id="app">
    <Nav></Nav>

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

      <smt-assignees>
      </smt-assignees>

      <div class="save">
        <button class="btn btn-primary" type="button" @@click="save" :disabled="this.saveState === 'working'">Speichern</button>
        <status-indicator :state="saveState" />
          <a class="btn btn-default" href='mailto:@assignees.map(_.email).flatten.mkString(",")'>E-Mail</a>
      </div>

    </div>
  </div>
</template>

<script>
import {req, bus} from './common'
import SmtTable from './components/SmtTable'
import SmtPlaceholder from './components/SmtPlaceholder'

export default {
  name: 'app',
  components: {
    SmtTable,
    SmtPlaceholder
  },
  props: ['api'],
  data: function () {
    return {
      plan: null,
      assignments: {
        sicherheit: [undefined, undefined],
        mikro: [undefined, undefined],
        tonanlage: [undefined]
      },
      saveState: '',
      current: null
    }
  },
  created: function () {
    console.log('created ', this.api)
    req.get(this.api).then(response => {
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
    }, err => console.log('failed to load plan ', err))
    bus.$on('change-serviceweek', date => {
      let dow = date.day() === 2 ? 5 : 2
      let formatted = date.format('YYYY-MM-DD')
      let part = this.plan.parts.find(p => p.date === formatted)
      if (this.current && (this.current.date === part)) {
        console.log('updating current')
        this.current.date = part.date
      } else {
        part.date = date.day(dow).format('YYYY-MM-DD')
      }
    })
    bus.$on('selected', dayPlan => {
      this.assignments = dayPlan.assignments
      this.current = dayPlan
    })
  },
  methods: {
    save: function () {
      this.saveState = 'working'
      req.put(this.url, this.plan).then(result => {
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

<style>
</style>
