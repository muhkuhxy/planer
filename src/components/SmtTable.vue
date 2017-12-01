<template>
  <div class="row termine">
    <div class="tr">
      <div class="th">Datum</div>
      <div class="th">Dienstwoche</div>
      <div class="th">Sicherheit</div>
      <div class="th">Mikro</div>
      <div class="th">Tonanlage</div>
    </div>
    <div class="tr" v-for="a in assignments" @click="select(a)" :class="{ 'bg-info': current === a }">
      <div class="cell main middle">
        <div>{{ a.date | dateLong }}</div>
      </div>
      <div class="cell main middle">
        <input v-if="dayOfWeek(a.date) !== 0" type="checkbox" :checked="dayOfWeek(a.date) === 2" @change.stop.prevent="change(a)" />
      </div>
      <div class="cell main col">
        <div class="cell sub">{{ a.sicherheit[0] }}</div>
        <div class="cell sub">{{ a.sicherheit[1] }}</div>
      </div>
      <div class="cell main col">
        <div class="cell sub">{{ a.mikro[0] }}</div>
        <div class="cell sub">{{ a.mikro[1] }}</div>
      </div>
      <div class="cell main middle">
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
  align-items: stretch;
}
.th {
  font-weight: bold;
  padding: 5px;
  border-bottom: 2px solid #ddd;
}
.cell {
  cursor: pointer;
  $padding: 5px;
  padding: $padding;
  min-height: calc(20px + #{2*$padding});
  border-bottom: 1px solid #ddd;
  &.col {
    padding: 0;
    display: flex;
    flex-flow: column nowrap;
    align-items: stretch;
  }
  &.sub:last-child {
    border-bottom: none;
  }
}
.th, .cell.main {
  &:first-child {
    width: 30%;
  }
  width: calc(70%/4);
}
.middle {
  display: flex;
  align-items: center;
}
</style>

<script>
import moment from 'moment'
import { mapState } from 'vuex'

export default {
  data: function () {
    return {
      today: moment().format('DD.MM.YYYY')
    }
  },
  computed: mapState(['assignments', 'current']),
  methods: {
    select (assignment) {
      this.$store.commit('select', assignment)
    },
    dayOfWeek (d) {
      return moment(d).day()
    },
    change (a) {
      this.$store.commit('toggleServiceweek', a)
    }
  }
}
</script>
