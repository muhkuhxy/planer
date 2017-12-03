import moment from 'moment'
import find from 'lodash/find'

const SLOTS = {
  sicherheit: 2,
  mikro: 2,
  tonanlage: 1
}

export default {
  select (state, current) {
    state.current = current
  },
  assign (state, payload) {
    const assignedPrevious = state.current[payload.service][payload.index]
    if (assignedPrevious) {
      find(state.assignees, a => a.name === assignedPrevious).timesAssigned -= 1
    }
    if (payload.name) {
      find(state.assignees, a => a.name === payload.name).timesAssigned += 1
    }
    state.current[payload.service][payload.index] = payload.name
    state.current[payload.service].splice(SLOTS[payload.service])
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
    const services = Object.keys(SLOTS)
    const timesAssigned = state.assignments.reduce((map, assignment) => {
      services.forEach(service => {
        assignment[service].forEach(name => {
          name && (name in map ? map[name]++ : map[name] = 1)
        })
      })
      return map
    }, {})
    state.assignees.forEach(a => {
      a.timesAssigned = a.name in timesAssigned ? timesAssigned[a.name] : 0
    })
  },
  loadAssignees (state, payload) {
    state.assignees = payload
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
