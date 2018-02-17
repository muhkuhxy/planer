<script>
import {mapState} from 'vuex'
import moment from 'moment'
import get from 'lodash/get'

export default {
  props: ['assignment'],
  methods: {
    name (serviceName, slot) {
      return get(this.assignment, [serviceName, slot, 'name'])
    },
    select () {
      this.$store.commit('select', this.assignment)
    },
    dayOfWeek () {
      return moment(this.assignment.date).day()
    },
    change () {
      this.$store.commit('toggleServiceweek', this.assignment)
    },
    remove (service, index) {
      this.$store.commit('assign', {
        name: '',
        index,
        service
      })
    }
  },
  computed: {
    selected () {
      return this.current === this.assignment
    },
    notSunday () {
      return this.dayOfWeek() !== 0
    },
    isTuesday () {
      return this.dayOfWeek() === 2
    },
    classObject () {
      return {
        platzhalter: this.selected
      }
    },
    ...mapState(['current', 'services'])
  }
}
</script>

<template>
  <div class="tr" @click="select" :class="{ 'bg-info': selected }">
    <div class="cell main vertical-middle">
      <div>{{ assignment.date | dateLong }}</div>
    </div>
    <div class="cell main vertical-middle serviceweek">
      <div>
        <toggle-button v-if="notSunday" :value="isTuesday" :sync="true" @change="change()" :height="17" :width="44"/>
      </div>
    </div>
    <div class="cell main col">
      <div class="cell sub" :class="classObject" data-service="sicherheit" data-index="0">{{ name('sicherheit', 0) }}
        <transition name="bounce">
          <div class="remove" v-if="selected && assignment.sicherheit[0]" @click="remove('sicherheit', 0)"><span class="badge"><span class="glyphicon glyphicon-remove"></span></span></div>
        </transition>
      </div>
      <div class="cell sub" :class="classObject" data-service="sicherheit" data-index="1">{{ name('sicherheit', 1) }}
        <transition name="bounce">
          <div class="remove" v-if="selected && assignment.sicherheit[1]" @click="remove('sicherheit', 1)"><span class="badge"><span class="glyphicon glyphicon-remove"></span></span></div>
        </transition>
      </div>
    </div>
    <div class="cell main col">
      <div class="cell sub" :class="classObject" data-service="mikro" data-index="0">{{ name('mikro', 0) }}
        <transition name="bounce">
          <div class="remove" v-if="selected && assignment.mikro[0]" @click="remove('mikro', 0)"><span class="badge"><span class="glyphicon glyphicon-remove"></span></span></div>
        </transition>
      </div>
      <div class="cell sub" :class="classObject" data-service="mikro" data-index="1">{{ name('mikro', 1) }}
        <transition name="bounce">
          <div class="remove" v-if="selected && assignment.mikro[1]" @click="remove('mikro', 1)"><span class="badge"><span class="glyphicon glyphicon-remove"></span></span></div>
        </transition>
      </div>
    </div>
    <div class="cell main vertical-middle" :class="classObject" data-service="tonanlage" data-index="0">
      <div>{{ name('tonanlage', 0) }}
        <transition name="bounce">
          <div class="remove" v-if="selected && assignment.tonanlage[0]" @click="remove('tonanlage', 0)"><span class="badge"><span class="glyphicon glyphicon-remove"></span></span></div>
        </transition>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@import '../assets/mixins';
@import '../assets/variables';
@import '~bootstrap-sass/assets/stylesheets/bootstrap/variables';
.tr {
  @include smt-table-row;
}
@media screen {
  .cell {
    cursor: pointer;
    $padding: 5px;
    padding: $padding;
    min-height: 32px;
    border-bottom: 1px solid #ddd;
    &.col {
      padding: 0;
      display: flex;
      flex-flow: column nowrap;
      align-items: stretch;
    }
    &.main {
      @include smt-column-width();
    }
    &.sub:last-child {
      border-bottom: none;
    }
  }
  .remove {
    float: right;
    .badge:hover {
      background-color: $state-danger-text;
    }
  }
  .platzhalter {
    border: 1px inset !important;
    position: relative;
  }
  $bounce: .075s;
  .bounce-enter-active {
    animation: bounce-in $bounce;
  }
  .bounce-leave-active {
    animation: bounce-in $bounce reverse;
  }
  @keyframes bounce-in {
    0% {
      transform: scale(0);
    }
    100% {
      transform: scale(1);
    }
  }
}
</style>
