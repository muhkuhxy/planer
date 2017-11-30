import {req} from '../lib/common'

export default {
  getPlan (id) {
    return req.get('/api/plan/' + id).then(response => {
      let plan = JSON.parse(response.responseText)
      plan.parts.forEach(p => {
        let {sicherheit = [], mikro = [], tonanlage = []} = p.assignments
        sicherheit.length = 2
        mikro.length = 2
        tonanlage.length = 1
        p.assignments = {
          sicherheit,
          mikro,
          tonanlage
        }
      })
      return plan
    })
  },
  list () {
    return req.get('/api/plan').then(r => JSON.parse(r.responseText))
  },
  save (plan) {
    if (!plan.id) {
      throw new Error('need plan with id')
    }
    return req.put('/api/plan/' + plan.id, plan)
  },
  getAssignees: function () {
    return req.get('/api/assignees').then(r => JSON.parse(r.responseText))
  }
}
