import {req} from './common'

export default {
  getPlan: function (id) {
    return req.get('/api/plan/' + id).then(response => {
      console.log(response)
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
      this.plan = plan
      this.current = plan ? plan.parts[0] : null
      return plan
    })
  },
  save: function (plan) {
    if (!plan.id) {
      throw new Error('need plan with id')
    }
    return req.put('/api/plan/' + plan.id, plan)
  },
  getAssignees: function () {
    return req.get('/api/assignees').then(r => JSON.parse(r.responseText))
  }
}
