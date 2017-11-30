import Vue from 'vue'
import VueRouter from 'vue-router'
import Smt from '../components/Smt'

Vue.use(VueRouter)

const routes = [
  {
    path: '/plan/:id',
    component: Smt
  }
]

export default new VueRouter({
  routes
})
