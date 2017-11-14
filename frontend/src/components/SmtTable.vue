<template>
  <div class="row termine" v-if="plan">
    <table class="table table-condensed">
      <thead>
        <tr>
          <th>Datum</th>
          <th class="serviceweek">Dienstwoche</th>
          <th>Sicherheit</th>
          <th>Mikro</th>
          <th>Tonanlage</th>
        </tr>
      </thead>
      <tbody>
        <template v-for="part in plan.parts">
          <smt-table-row-one :dayPlan="part" @selected="select" :class="{ info: current === part }">
          </smt-table-row-one>
          <tr @click="select(part)" :class="{ info: current === part }">
            <td class="sicherheit">{{ part.assignments.sicherheit[1] }}</td>
            <td class="mikro">{{ part.assignments.mikro[1] }}</td>
          </tr>
        </template>
      </tbody>
    </table>

    <div class="printing-date">{{ today }}</div>
  </div>
</template>

<script>
import SmtTableRowOne from './SmtTableRowOne'
import moment from 'moment'
import {bus} from '../common'

export default {
  name: 'smt-table',
  props: ['plan', 'current'],
  data: function () {
    return {
      today: moment().format('DD.MM.YYYY')
    }
  },
  methods: {
    select: function (a) {
      bus.$emit('selected', a)
      console.log('emitting ', a)
    }
  },
  components: {
    SmtTableRowOne
  }
}
</script>

<style>
</style>
