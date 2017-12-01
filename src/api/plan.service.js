import {req} from '../lib/common'

export default {
  getPlan (id) {
    return req.get('/api/plan/' + id).then(response => {
      let plan = JSON.parse(response.responseText)
      plan.parts = plan.parts.map(p => {
        let {sicherheit = [], mikro = [], tonanlage = []} = p.assignments
        sicherheit.length = 2
        mikro.length = 2
        tonanlage.length = 1
        let {id, date, serviceweek, unavailable} = p
        return {id, date, sicherheit, mikro, tonanlage, serviceweek, unavailable}
      })
      return plan
    })
  },
  list () {
    return req.get('/api/plan').then(r => JSON.parse(r.responseText))
  },
  save (plan) {
    return req.put('/api/plan/' + plan[0], plan)
  },
  getAssignees: function () {
    return req.get('/api/assignees').then(r => JSON.parse(r.responseText))
  }
}
