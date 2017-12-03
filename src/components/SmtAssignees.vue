<script>
import {$, $$} from '../lib/common'
import Assignee from './Assignee.vue'
import {mapState} from 'vuex'

export default {
  props: ['assignees'],
  computed: {
    ...mapState(['current', 'assignments']),
    blocking () {
      return !(this.assignments && this.assignments.indexOf(this.current) > -1)
    }
  },
  components: {
    Assignee
  },
  created: function () {
    let dragged

    document.addEventListener('dragstart', e => {
      dragged = e.target
      if (e.target.classList.contains('disabled') ||
        e.target.classList.contains('assigned')) {
        e.preventDefault()
        return
      }
      e.dataTransfer.setData('text/plain', null)
      e.target.style.opacity = 0.5
    })

    document.addEventListener('dragend', e => {
      dragged.style.opacity = ''
    })

    function classesOverlap (placeholder) {
      return $$('.dienste span', dragged).map(span => span.className).includes(placeholder.dataset.service)
    }

    document.addEventListener('dragenter', e => {
      if (e.target.classList.contains('platzhalter') && classesOverlap(e.target, dragged)) {
        e.target.style.backgroundColor = '#aaa'
      } else {
        e.preventDefault()
      }
    }, false)

    document.addEventListener('dragover', e => {
      e.preventDefault()
    })

    document.addEventListener('dragleave', e => {
      if (!e.target || !e.target.style) {
        return
      }
      e.target.style.backgroundColor = ''
    })

    document.addEventListener('drop', e => {
      e.preventDefault()
      if (e.target.classList.contains('platzhalter') && classesOverlap(e.target)) {
        e.target.style.backgroundColor = ''
        this.$store.commit('assign', {
          name: $('.name', dragged).textContent,
          service: e.target.dataset.service,
          index: e.target.dataset.index
        })
      }
    })
  }
}
</script>

<template>
  <div class="row kandidaten">
    <div class="col-xs-12 flex">
      <h2>Helfer</h2>
      <ul class="draggable">
        <li v-for="assignee in assignees">
          <Assignee :ae="assignee"/>
        </li>
      </ul>
      <div v-if="blocking" class="blocker">Datum auswählen</div>
    </div>
  </div>
</template>

<style lang="scss">
@import '../assets/mixins';
@import '../assets/variables';

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
    background-color: #fafafa;
    box-shadow: 1px 1px 4px #777;

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
