<template>
  <div class="row termine" v-if="plan">
    <div class="tr">
      <div class="th">Datum</div>
      <div class="th">Dienstwoche</div>
      <div class="th">Sicherheit</div>
      <div class="th">Mikro</div>
      <div class="th">Tonanlage</div>
    </div>
    <div class="tr" v-for="a in assignments" @click="select(a.date)" :class="{ 'bg-info': current === a.date }">
      <div class="td">{{ a.date | dateLong }}</div>
      <div class="td">
        <input v-if="dayOfWeek(a.date) !== 0" type="checkbox" :checked="dayOfWeek(a.date) === 2" @change.stop.prevent="change(a)" />
      </div>
      <div class="td col">
        <div>{{ a.sicherheit[0] }}</div>
        <div>{{ a.sicherheit[1] }}</div>
      </div>
      <div class="td col">
        <div>{{ a.mikro[0] }}</div>
        <div>{{ a.mikro[1] }}</div>
      </div>
      <div class="td col">
        <div>{{ a.tonanlage[0] }}</div>
      </div>
    </div>

    <div class="printing-date">Stand vom {{ today }}</div>
  </div>
</template>

<style lang="scss" scoped>
.tr {
  display: flex;
  width: 100%;
}
.tr > * {
  width: 20%;
}
</style>

<script>
import moment from 'moment'

export default {
  name: 'smt-table',
  data: function () {
    return {
      today: moment().format('DD.MM.YYYY')
    }
  },
  computed: {
    plan () {
      return this.$store.state.parts
    },
    assignments () {
      return this.$store.state.assignments
    },
    current () {
      return this.$store.state.current
    }
  },
  methods: {
    select (date) {
      console.log(date)
      this.$store.commit('select', date)
    },
    dayOfWeek (d) {
      return moment(d).day()
    },
    change (a) {
      this.$store.commit('change-serviceweek', a)
    }
  }
}
</script>
