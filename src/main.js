import Vue from 'vue'
import moment from 'moment'
import store from './store'
import router from './router'
import App from './App'
import ToggleButton from 'vue-js-toggle-button'

Vue.config.productionTip = false

moment.locale('de', {
  weekdays: ['Sonntag', 'Montag', 'Dienstag', 'Mittwoch', 'Donnerstag', 'Freitag', 'Samstag']
})
moment.locale('de')

Vue.filter('dateLong', function (value) {
  if (!value) return ''
  return moment(value).format('dddd, DD.MM.YYYY')
})

Vue.use(ToggleButton)

// eslint-disable-next-line no-new
new Vue({
  el: '#app',
  router,
  store,
  render: h => h(App)
})
