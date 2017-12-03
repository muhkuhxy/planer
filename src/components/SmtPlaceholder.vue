<script>
import Vue from 'vue'
import {mapState} from 'vuex'

const ph = {
  props: ['service'],
  computed: mapState({
    assignment (state) {
      return state.current && state.current[this.service]
    }
  }),
  template: `
  <div class="col-md-4" :class="service">
    <h3>{{ service }}</h3>
      <div class="platzhalter" v-for="(name, index) in assignment" :data-service="service" :data-index="index" :class="{'bg-success': name}">
        <div class="remove" @click="remove(index)">
          <span class="glyphicon glyphicon-remove"></span>
        </div>
        {{ name }}
    </div>
  </div>
  `,
  methods: {
    remove (index) {
      this.$store.commit('assign', {
        name: '',
        index,
        service: this.service
      })
    }
  }
}

export default Vue.component('smt-placeholder', {
  computed: mapState(['current']),
  components: {
    ph
  }
})
</script>

<template>
  <div class="row planung" v-if="current">
  <h2 class="col-xs-12">Plan für <span>{{ current.date | dateLong }}</span></h2>
  <ph service="sicherheit"></ph>
  <ph service="mikro"></ph>
  <ph service="tonanlage"></ph>
  </div>
</template>

<style lang="scss">
@import '../assets/variables';

.platzhalter {
  width: 100%;
  height: 5em;
  text-align: center;
  padding: 2em 0;
  margin-bottom: 1em;
  position: relative;

  .remove {
    position: absolute;
    top: 3px;
    right: 5px;
    font-weight: bold;
    cursor: pointer;
  }

  &:not(.bg-success) {
    background-color: $color-platzhalter;
  }

}

@each $dienst in $dienste {
  .#{$dienst} h3 {
    color: map-get($colors, $dienst);
  }
}
</style>
