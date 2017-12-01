import Vue from 'vue'
import moment from 'moment'
import findIndex from 'lodash/findIndex'

const slots = {
  sicherheit: 2,
  mikro: 2,
  tonanlage: 1
}

export default {
  select (state, current) {
    state.current = current
  },
  assign (state, payload) {
    state.current[payload.service][payload.index] = payload.name
    state.current[payload.service].splice(slots[payload.service])
    let assignmentIndex = findIndex(state.assignments, a => a.date === state.current)
    Vue.set(state.assignments, assignmentIndex, state.current)
  },
  toggleUnavailable (state, name) {
    const list = state.current.unavailable
    const index = list.indexOf(name)
    if (index !== -1) {
      list.splice(index, 1)
    } else {
      list.push(name)
    }
  },
  load (state, payload) {
    state.assignments = payload.parts
    state.title = payload.name
    state.current = state.assignments[0]
  },
  toggleServiceweek (state, dayPlan) {
    const date = moment(dayPlan.date)
    const isServiceweek = date.day() === 2
    let newDate = date.day(isServiceweek ? 5 : 2).format('YYYY-MM-DD')
    if (dayPlan.date === state.current) {
      state.current = newDate
    }
    dayPlan.date = newDate
    dayPlan.serviceweek = !isServiceweek
  },
  login (state, user) {
    state.user = user
  },
  logout (state) {
    state.user = ''
  }
}
