import Vue from 'vue'
import moment from 'moment'

export default Vue.component('smt-placeholder', {
  props: ['assignments', 'date'],
  computed: {
    formatted: function () {
      return moment(this.date).format('dddd, DD.MM.YYYY')
    }
  },
  template: `
    <div class="row planung" v-if="date">
       <h2 class="col-xs-12">Plan für <span>{{ formatted }}</span></h2>
       <ph name="sicherheit" :assignment="assignments.sicherheit"></ph>
       <ph name="mikro" :assignment="assignments.mikro"></ph>
       <ph name="tonanlage" :assignment="assignments.tonanlage"></ph>
    </div>
  `,
  components: {
    ph: {
      props: ['name', 'assignment'],
      methods: {
        remove: function () {
        }
      },
      template: `
       <div class="col-md-4" :class="name">
          <h3>{{ name }}</h3>
          <div class="platzhalter" v-for="ass in assignment">
             <div class="remove" @click="remove()">
                <span class="glyphicon glyphicon-remove"></span>
             </div>
             <div class="slot"></div>
          </div>
       </div>
      `
    }
  }
})
