import Vue from 'vue'
import Vuex from 'vuex'
import mutations from './mutations'
import actions from './actions'

Vue.use(Vuex)

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
  strict: process.env.NODE_ENV !== 'production'
})
