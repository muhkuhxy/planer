<script>
import Vue from 'vue'

const ph = {
  props: ['service', 'assignment'],
  methods: {
    remove: function () {
    }
  },
  template: `
  <div class="col-md-4" :class="service">
    <h3>{{ service }}</h3>
      <div class="platzhalter" :data-service="service" :data-index="index" v-for="(ass, index) in assignment">
        <div class="remove" @click="remove()">
          <span class="glyphicon glyphicon-remove"></span>
        </div>
        {{ ass }}
    </div>
  </div>
  `
}

export default Vue.component('smt-placeholder', {
  props: ['assignments', 'date'],
  components: {
    ph
  }
})
</script>

<template>
  <div class="row planung" v-if="date">
  <h2 class="col-xs-12">Plan für <span>{{ date | dateLong }}</span></h2>
  <ph service="sicherheit" :assignment="assignments.sicherheit"></ph>
  <ph service="mikro" :assignment="assignments.mikro"></ph>
  <ph service="tonanlage" :assignment="assignments.tonanlage"></ph>
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
