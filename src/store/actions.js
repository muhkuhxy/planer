import service from '../api/plan.service'
import {handleUnauthorized} from '../lib/appHelpers'

export default {
  async loadPlan ({ commit }, id) {
    try {
      const assignees = await service.getAssignees()
      const plan = await service.getPlan(id, assignees)
      commit('loadAssignees', assignees)
      commit('load', plan)
    } catch (err) {
      handleUnauthorized(err)
      commit('load', null)
    }
  }
}
