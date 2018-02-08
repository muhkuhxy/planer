import {req} from '../lib/common'

export default {
  getPlan (id, assignees) {
    const indexedAssignees = assignees.reduce((map, value) => {
      map[value.id] = value
      return map
    }, {})
    return req.get('/api/plan/' + id).then(response => {
      let plan = JSON.parse(response.responseText)
      plan.parts = plan.parts.map(p => {
        let {id, date, serviceweek, unavailable} = p
        let part = {id, date, serviceweek, unavailable}
        plan.services.forEach(service => {
          let assignment = p.assignments.find(elem => elem.s === service.id)
          let shifts = assignment ? assignment.shifts || [] : []
          shifts.length = service.slots
          part[service.name] = shifts.map(s => indexedAssignees[s])
        })
        return part
      })
      return plan
    })
  },
  list () {
    return req.get('/api/plan').then(r => JSON.parse(r.responseText))
  },
  save (plan) {
    return req.put('/api/plan/' + plan.id, plan)
  },
  getAssignees: function () {
    return req.get('/api/assignees').then(r => JSON.parse(r.responseText))
  },
  saveAssignees (assignees) {
    return req.put('/api/assignees', assignees)
  }
}
