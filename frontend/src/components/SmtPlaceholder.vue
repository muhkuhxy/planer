<script>
import Vue from 'vue'
import dp from 'dot-prop'

const ph = {
  props: ['service'],
  computed: {
    assignment () {
      return dp.get(this.$store.state.currentAssignment, this.service)
    }
  },
  template: `
  <div class="col-md-4" :class="service">
    <h3>{{ service }}</h3>
      <div class="platzhalter" :data-service="service" :data-index="index" v-for="(name, index) in assignment">
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
  computed: {
    current () {
      return dp.get(this.$store.state, 'current')
    },
    currentAssignment () {
      return dp.get(this.$store.state.currentAssignment)
    }
  },
  components: {
    ph
  }
})
</script>

<template>
  <div class="row planung" v-if="current">
  <h2 class="col-xs-12">Plan für <span>{{ current | dateLong }}</span></h2>
  <ph service="sicherheit"></ph>
  <ph service="mikro"></ph>
  <ph service="tonanlage"></ph>
  </div>
</template>

<style lang="scss">
@import '../assets/settings';

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

  @include border-radius;
}

@each $dienst in $dienste {
  .#{$dienst} h3 {
    color: map-get($colors, $dienst);
  }
}
</style>
