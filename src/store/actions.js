import service from '../api/plan.service'
import {handleUnauthorized} from '../lib/appHelpers'

export default {
  async loadPlan ({ commit }, id) {
    try {
      const plan = await service.getPlan(id)
      const assignees = await service.getAssignees()
      commit('loadAssignees', assignees)
      commit('load', plan)
    } catch (err) {
      handleUnauthorized(err)
    }
  }
}
