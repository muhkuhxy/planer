<script>
import {$$} from '../common'

export default {
  props: ['assignees', 'current'],
  components: {
    assignee: {
      template: `
        <div>
          <span class="name">{{ ae.name }}</span>
          <input class="pull-right" name="unavailable" value="" type="checkbox">
          <div class="dienste">
            <div class="counter pull-left">0</div>
            <span :class="s" v-for="s in ae.services">{{ s[0] }}</span>
          </div>
        </div>
      `,
      props: ['ae']
    }
  },
  created: function () {
//    service.options('assignees', {
//      isContainer: function (el) {
//        return el.classList && el.classList.contains('platzhalter')
//      },
//      accepts: function (el, target, source) {
//        console.log('accepts', el, target, source)
//        return $$('.dienste span', el).map(span => span.className).includes(target.dataset.service)
//      },
//      copy: true
//    })
  }
}
</script>

<template>
  <div class="row kandidaten">
    <div class="col-xs-12 flex">
      <h2>Helfer</h2>
      <ul class="draggable">
        <li v-for="assignee in assignees">
          <assignee :ae="assignee"/>
        </li>
      </ul>
      <div v-if="!current" class="blocker">Datum auswählen</div>
    </div>
  </div>
</template>

<style lang="scss">
@import '../assets/settings';

.kandidaten {

  position: relative;

  .blocker {
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    right: 0;
    background-color: rgba(228, 213, 214, 0.9);
    color: black;
    font-size: 200%;
    text-align: center;
    display: flex;
    justify-content: center;
    flex-direction: column;
  }

  ul {
    list-style: none;
    display: flex;
    flex-wrap: wrap;
    padding: 0;
  }

  li {
    width: 20%;
  }

  li > div {
    margin: 0.5em;
    padding: 0.5em;
    border: 1px solid black;
    @include border-radius;
    background-color: white;

    &.disabled {
      background-color: $color-disabled;
      opacity: .5;
    }

    &.assigned {
      background-color: $color-assigned;
    }
  }

  .dienste {
    margin-top: 1em;
    text-align: right;
    color: white;

    .counter {
      color: black;
    }

    span {
      display: inline-block;
      margin-left: 2px;
      width: 2em;
      text-align: center;
    }
  }

  @each $dienst in $dienste {
    .#{$dienst} {
      background-color: map-get($colors, $dienst);
    }
  }

}
</style>
