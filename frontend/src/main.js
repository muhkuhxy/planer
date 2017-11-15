// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import App from './App'
import router from './router'
import moment from 'moment'

Vue.config.productionTip = false

const api = 'http://localhost:8080/api/plan/19'

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
  template: '<App :api="api"/>',
  components: { App },
  data: {
    api
  }
})
