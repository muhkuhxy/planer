<script>
import moment from 'moment'
import { mapState } from 'vuex'
import SmtTableBody from './SmtTableBody'

export default {
  data: function () {
    return {
      today: moment().format('DD.MM.YYYY')
    }
  },
  computed: mapState(['assignments', 'current', 'title']),
  components: {
    SmtTableBody
  }
}
</script>

<template>
  <div class="row termine">
    <div class="col-xs-12">
      <h3 class="title">{{ title }}</h3>
      <div class="tr">
        <div class="th">Datum</div>
        <div class="th serviceweek">Dienstwoche</div>
        <div class="th sicherheit">Sicherheit</div>
        <div class="th mikro">Mikro</div>
        <div class="th ton">Tonanlage</div>
      </div>
      <SmtTableBody :key="a.id" v-for="a in assignments" :assignment="a"></SmtTableBody>
      <div class="printing-date">Stand vom {{ today }}</div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@import '../assets/mixins';
@import '../assets/variables';
.tr {
  @include smt-table-row();
}
.th {
  font-weight: bold;
}
@media screen {
  .th {
    @include smt-column-width();
    padding: 5px;
    border-bottom: 2px solid #ddd;
  }
  .sicherheit {
    color: $color-sicherheit;
  }
  .mikro {
    color: $color-mikro;
  }
  .ton {
    color: $color-tonanlage;
  }
  .termine {
    max-height: 300px;
    overflow-y: scroll;
    margin-bottom: 2rem;
  }
  .printing-date {
    display: none;
  }
}
</style>
