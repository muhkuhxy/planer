<template>
  <tr @click="select">
    <td class="datum" rowspan="2">{{ dayPlan.date | dateLong }}</td>
    <td class="serviceweek" rowspan="2">
      <input v-if="dayOfWeek !== 0" type="checkbox" :checked="dayOfWeek === 2" @change.stop.prevent="change" />
    </td>
    <td class="sicherheit">{{ dayPlan.assignments.sicherheit[0] }}</td>
    <td class="mikro">{{ dayPlan.assignments.mikro[0] }}</td>
    <td class="tonanlage" rowspan="2">{{ dayPlan.assignments.tonanlage[0] }}</td>
  </tr>
</template>

<script>
import {bus} from '../common'
import moment from 'moment'

export default {
  name: 'smt-table-row-one',
  props: ['dayPlan'],
  computed: {
    dayOfWeek: function () {
      return this.date.day()
    },
    date: function () {
      return moment(this.dayPlan.date)
    },
    formattedDate: function () {
      return this.date.format('dddd, DD.MM.YYYY')
    }
  },
  methods: {
    change: function () {
      bus.$emit('change-serviceweek', this.dayPlan)
    },
    select: function () {
      bus.$emit('selected', this.dayPlan)
    }
  }
}
</script>
