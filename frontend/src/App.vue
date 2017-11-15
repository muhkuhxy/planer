<template>
  <div id="app">
    <Nav></Nav>

    <div class="container" id="wrapper">

      <header class="row" v-if="plan">
        <div class="col-xs-12">
          <h2>{{ plan.name }}</h2>
        </div>
      </header>

      <smt-table :plan="plan" :current="current">
      </smt-table>

      <smt-placeholder :assignments="assignments" v-if="current" :date="current.date">
      </smt-placeholder>

      <smt-assignees>
      </smt-assignees>

      <div class="save">
        <button class="btn btn-primary" type="button" @click="save" :disabled="this.saveState === 'working'">Speichern</button>
        <status-indicator :state="saveState" />
          <a class="btn btn-default" href='mailto:@assignees.map(_.email).flatten.mkString(",")'>E-Mail</a>
      </div>

    </div>
  </div>
</template>

<script>
import {req, bus} from './common'
import SmtTable from './components/SmtTable'
import SmtPlaceholder from './components/SmtPlaceholder'
import StatusIndicator from './components/StatusIndicator'

// import 'bootstrap-sass/assets/stylesheets/_bootstrap.scss'
// import './assets/global-styles.scss'

export default {
  name: 'app',
  components: {
    SmtTable,
    SmtPlaceholder,
    StatusIndicator
  },
  props: ['api'],
  data: function () {
    return {
      plan: null,
      assignments: {
        sicherheit: [undefined, undefined],
        mikro: [undefined, undefined],
        tonanlage: [undefined]
      },
      saveState: '',
      current: null
    }
  },
  created: function () {
    console.log('created ', this.api)
    req.get(this.api).then(response => {
      console.log(response)
      let plan = JSON.parse(response.responseText)
      plan.parts.forEach(p => {
        let {sicherheit = [], mikro = [], tonanlage = []} = p.assignments
        sicherheit.length = 2
        mikro.length = 2
        tonanlage.length = 1
        p.assignments = {
          sicherheit: sicherheit,
          mikro: mikro,
          tonanlage: tonanlage
        }
      })
      this.plan = plan
    }, err => console.log('failed to load plan ', err))
    bus.$on('change-serviceweek', date => {
      let dow = date.day() === 2 ? 5 : 2
      let formatted = date.format('YYYY-MM-DD')
      let part = this.plan.parts.find(p => p.date === formatted)
      if (this.current && (this.current.date === part)) {
        console.log('updating current')
        this.current.date = part.date
      } else {
        part.date = date.day(dow).format('YYYY-MM-DD')
      }
    })
    bus.$on('selected', dayPlan => {
      this.assignments = dayPlan.assignments
      this.current = dayPlan
    })
  },
  methods: {
    save: function () {
      this.saveState = 'working'
      req.put('/api/plan/' + this.plan.id, this.plan).then(result => {
        this.saveState = result.status === 200 ? 'success' : 'error'
        console.log(result)
      }, err => {
        this.saveState = 'error'
        console.log(err)
      })
    }
  }
}
</script>

<style lang="scss">
$icon-font-path: '~bootstrap-sass/assets/fonts/bootstrap/';
@import '~bootstrap-sass/assets/stylesheets/_bootstrap.scss';
@import "./assets/print";

$color-sicherheit: dodgerblue;
$color-mikro: forestgreen;
$color-tonanlage: deeppink;
$color-platzhalter: gainsboro;
$color-active: #d9edf7;
$color-disabled: #dcadad;
$color-assigned: gainsboro;

$colors: (
    sicherheit: dodgerblue,
    mikro: forestgreen,
    tonanlage: deeppink
);
$dienste: sicherheit, mikro, tonanlage;


.date-range {
   margin-bottom: 1em;
}

@mixin border-radius {
   border-radius: 5px;
}

.planung {

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

}

.termine {

   tbody td {
      vertical-align: middle !important;
   }

   tbody tr {
      cursor: pointer;
   }

   @media screen {
      max-height: 200px;
      overflow-y: scroll;

      .printing-date {
         display: none;
      }
   }

}

.save {
  padding: 3rem;
  display: flex;
  justify-content: center;

  .btn, div {
    margin-right: .3em;
  }
}


</style>
