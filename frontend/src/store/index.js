import Vue from 'vue'
import Vuex from 'vuex'
import mutations from './mutations'
import actions from './actions'

Vue.use(Vuex)

const getters = {
  servicesByName (state) {
    return state.services && state.services.reduce((map, service) => {
      map[service.name] = service
      return map
    }, {})
  }
}

export default new Vuex.Store({
  state: {
    user: '',
    title: '',
    current: null,
    assignees: [],
    assignments: [],
    services: []
  },
  mutations,
  actions,
  getters,
  strict: process.env.NODE_ENV !== 'production'
})
