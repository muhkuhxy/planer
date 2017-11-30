import Vue from 'vue'
import VueRouter from 'vue-router'
import Smt from '../components/Smt'
import LoginForm from '../components/LoginForm'

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    component: LoginForm
  },
  {
    path: '/plan/:id',
    component: Smt
  }
]

export default new VueRouter({
  routes
})
