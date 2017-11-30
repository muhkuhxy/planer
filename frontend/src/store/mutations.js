import Vue from 'vue'
import moment from 'moment'
import findIndex from 'lodash/findIndex'

const blueprint = {
  sicherheit: 2,
  mikro: 2,
  tonanlage: 1
}

export default {
  select: function (state, current) {
    state.current = current
    state.currentAssignment = state.assignments.find(a => a.date === state.current)
  },
  assign: function (state, payload) {
    state.currentAssignment[payload.service][payload.index] = payload.name
    state.currentAssignment[payload.service].splice(blueprint[payload.service])
    let assignmentIndex = findIndex(state.assignments, a => a.date === state.current)
    Vue.set(state.assignments, assignmentIndex, state.currentAssignment)
  },
  toggleUnavailable: function (state, name) {
    const index = state.currentAssignment.unavailable.indexOf(name)
    if (index !== -1) {
      state.currentAssignment.unavailable.splice(index, 1)
    } else {
      state.currentAssignment.unavailable.push(name)
    }
  },
  load: function (state, payload) {
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
  'change-serviceweek': function (state, dayPlan) {
    const date = moment(dayPlan.date)
    let dow = date.day() === 2 ? 5 : 2
    if (dayPlan.date === state.current) {
      state.current = date.day(dow).format('YYYY-MM-DD')
    }
    dayPlan.date = date.day(dow).format('YYYY-MM-DD')
  }
}
