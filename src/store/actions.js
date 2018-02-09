import service from '../api/plan.service'
import {handleUnauthorized} from '../lib/appHelpers'

export default {
  async loadPlan ({ commit }, id) {
    try {
      const assignees = await service.getAssignees()
      commit('loadAssignees', assignees)
      const plan = await service.getPlan(id, assignees)
      commit('loadServices', plan.services)
      commit('load', plan)
    } catch (err) {
      handleUnauthorized(err)
      commit('load', null)
    }
  },
  async loadAssignees ({ commit }) {
    try {
      const assignees = await service.getAssignees()
      commit('loadAssignees', assignees)
      const services = await service.getServices()
      commit('loadServices', services)
    } catch (err) {
      handleUnauthorized(err)
      commit('loadServices', null)
      commit('loadAssignees', null)
    }
  }
}
