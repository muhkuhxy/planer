<script>
import {mapState} from 'vuex'

export default {
  props: ['ae'],
  computed: {
    classObject () {
      return {
        disabled: this.current && this.current.unavailable.includes(this.ae.name),
        assigned: this.current && this.ae.services.some(s => this.current[s].includes(this.ae.name))
      }
    },
    ...mapState(['current'])
  },
  methods: {
    unavailable () {
      this.$store.commit('toggleUnavailable', this.ae.name)
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
      <span :class="s" v-for="s in ae.services">{{ s[0] }}</span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
</style>
