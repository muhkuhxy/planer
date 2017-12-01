import Vue from 'vue'
import moment from 'moment'
import store from './store'
import router from './router'
import Smt from './components/Smt'
import AppNav from './components/AppNav'

Vue.config.productionTip = false

moment.locale('de', {
  weekdays: ['Sonntag', 'Montag', 'Dienstag', 'Mittwoch', 'Donnerstag', 'Freitag', 'Samstag']
})
moment.locale('de')

Vue.filter('dateLong', function (value) {
  if (!value) return ''
  return moment(value).format('dddd, DD.MM.YYYY')
})

// eslint-disable-next-line no-new
new Vue({
  el: '#app',
  router,
  store,
  template: `
  <div id="app">
    <AppNav></AppNav>
    <router-view></router-view>
  </div>
  `,
  components: { Smt, AppNav }
})
