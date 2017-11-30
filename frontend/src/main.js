// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import Vuex from 'vuex'
import Smt from './components/Smt'
import VueRouter from 'vue-router'
import moment from 'moment'
import findIndex from 'lodash/findIndex'
// import {$} from './common'

Vue.config.productionTip = false

moment.locale('de', {
  weekdays: ['Sonntag', 'Montag', 'Dienstag', 'Mittwoch', 'Donnerstag', 'Freitag', 'Samstag']
})
moment.locale('de')

Vue.filter('dateLong', function (value) {
  if (!value) return ''
  return moment(value).format('dddd, DD.MM.YYYY')
})

const routes = [
  {
    path: '/plan/:id',
    component: Smt
  }
]

Vue.use(Vuex)
Vue.use(VueRouter)

const router = new VueRouter({
  routes
})

const blueprint = {
  sicherheit: 2,
  mikro: 2,
  tonanlage: 1
}

const store = new Vuex.Store({
  state: {
    title: '',
    parts: [],
    current: null,
    assignments: [],
    currentAssignment: null
  },
  mutations: {
    select (state, current) {
      state.current = current
      state.currentAssignment = state.assignments.find(a => a.date === state.current)
    },
    assign (state, payload) {
      state.currentAssignment[payload.service][payload.index] = payload.name
      state.currentAssignment[payload.service].splice(blueprint[payload.service])
      let assignmentIndex = findIndex(state.assignments, a => a.date === state.current)
      Vue.set(state.assignments, assignmentIndex, state.currentAssignment)
    },
    toggleUnavailable (state, name) {
      const index = state.currentAssignment.unavailable.indexOf(name)
      if (index !== -1) {
        state.currentAssignment.unavailable.splice(index, 1)
      } else {
        state.currentAssignment.unavailable.push(name)
      }
    },
    load (state, payload) {
      state.assignments = payload.parts.map(p => {
        let {date, assignments, serviceweek, unavailable} = p
        for (let s in blueprint) {
          assignments[s].splice(blueprint[s])
        }
        return {date, ...assignments, serviceweek, unavailable}
      })
      state.parts = payload.parts
      state.title = payload.name
      state.currentAssignment = state.assignments[0]
      state.current = state.currentAssignment.date
    },
    'change-serviceweek' (state, dayPlan) {
      const date = moment(dayPlan.date)
      let dow = date.day() === 2 ? 5 : 2
      if (dayPlan.date === state.current) {
        state.current = date.day(dow).format('YYYY-MM-DD')
      }
      dayPlan.date = date.day(dow).format('YYYY-MM-DD')
    }
  }
})

// eslint-disable-next-line no-new
new Vue({
  el: '#app',
  router,
  store,
  template: `
  <div id="app">
    <app-nav></app-nav>
    <router-view></router-view>
  </div>
  `,
  components: { Smt }
})
