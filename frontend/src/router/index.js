import Vue from 'vue'
import VueRouter from 'vue-router'
import Smt from '../components/Smt'
import SmtPlans from '../components/SmtPlans'
import LoginForm from '../components/LoginForm'
import AssigneeManager from '../components/AssigneeManager'

Vue.use(VueRouter)

const routes = [
  {
    path: '*',
    redirect: '/plan'
  },
  {
    path: '/login',
    component: LoginForm
  },
  {
    path: '/plan',
    component: SmtPlans
  },
  {
    name: 'plan',
    path: '/plan/:id',
    component: Smt,
    props: true
  },
  {
    path: '/assignees',
    component: AssigneeManager
  }
]

export default new VueRouter({
  routes
})
