<script>
import {mapState} from 'vuex'

export default {
  props: ['ae'],
  computed: {
    classObject () {
      return {
        disabled: this.current && this.current.unavailable.includes(this.ae.id),
        assigned: this.current && this.ae.services.some(s => this.current[this.serviceName(s)].includes(this.ae))
      }
    },
    ...mapState(['current', 'services'])
  },
  methods: {
    unavailable () {
      this.$store.commit('toggleUnavailable', this.ae.id)
    },
    serviceName (id) {
      return this.services.find(x => x.id === id).name
    }
  }
}
</script>

<template>
  <div draggable="true" :class="classObject">
    <span class="name">{{ ae.name }}</span>
    <input class="pull-right" :disabled="classObject.assigned" @change="unavailable" :checked="classObject.disabled" type="checkbox">
    <div class="dienste">
      <div class="counter pull-left">{{ ae.timesAssigned }}</div>
      <span :class="serviceName(s)" v-for="s in ae.services">{{ serviceName(s)[0] }}</span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
</style>
