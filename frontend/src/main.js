// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import Vuex from 'vuex'
import App from './App'
import router from './router'
import moment from 'moment'
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

Vue.use(Vuex)

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
      state.parts.find(p => p.date === state.current).assignments[payload.service][payload.index] = payload.name
    },
    load (state, payload) {
      state.assignments = payload.parts.map(p => {
        let {date, assignments, serviceweek} = p
        for (let s in blueprint) {
          assignments[s].length = blueprint[s]
        }
        return {date, ...assignments, serviceweek}
      })
      state.parts = payload.parts
      state.title = payload.name
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
  template: '<App/>',
  components: { App }
})
