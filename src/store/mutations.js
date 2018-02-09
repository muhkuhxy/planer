import moment from 'moment'
import find from 'lodash/find'

export default {
  select (state, current) {
    state.current = current
  },
  assign (state, payload) {
    const assignedPrevious = state.current[payload.service][payload.index]
    if (assignedPrevious) {
      assignedPrevious.timesAssigned -= 1
    }
    const assigned = payload.name ? find(state.assignees, a => a.name === payload.name) : undefined
    if (assigned) {
      assigned.timesAssigned += 1
    }
    state.current[payload.service][payload.index] = assigned
    const slots = find(state.services, s => s.name === payload.service).slots
    state.current[payload.service].splice(slots)
  },
  toggleUnavailable (state, id) {
    const list = state.current.unavailable
    const index = list.indexOf(id)
    if (index !== -1) {
      list.splice(index, 1)
    } else {
      list.push(id)
    }
  },
  load (state, payload) {
    state.assignments = payload.parts
    state.title = payload.name
    const timesAssigned = state.assignments.reduce((map, assignment) => {
      state.services.forEach(service => {
        assignment[service.name].forEach(a => {
          a && (a.name in map ? map[a.name]++ : map[a.name] = 1)
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
  loadServices (state, payload) {
    state.services = payload
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
