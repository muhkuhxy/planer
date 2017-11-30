import Vue from 'vue'
import Vuex from 'vuex'
import mutations from './mutations'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    title: '',
    parts: [],
    current: null,
    assignments: [],
    currentAssignment: null
  },
  mutations
})
