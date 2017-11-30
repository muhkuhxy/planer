<script>
import service from '../api/plan.service'

export default {
  data () {
    return {
      plans: [],
      loading: true
    }
  },
  created () {
    this.load()
  },
  methods: {
    load () {
      service.list().then(
        ps => {
          this.plans = ps
          this.loading = false
        },
        err => {
          if (err.message === 'Unauthorized') {
            this.$router.push('login')
            console.log('unuath')
          } else {
            for (let f in err) {
              console.log(f)
            }
          }
        }
      )
    }
  }
}
</script>

<template>
  <div class="container">

    <header class="row">
      <div class="col-xs-12">
        <h2>Pläne</h2>
      </div>
    </header>

    <ul class="plan-list">
      <li v-for="plan in plans">{{ plan.name }}
        <router-link class="btn btn-info btn-sm" :to="{ name: 'plan', params: { id: plan.id } }">Bearbeiten</router-link>
        <button class="plan-remove btn btn-warning btn-sm" @click="remove(plan.id)">Löschen</button>
      </li>
    </ul>

    <div class="row date-range">
      <div class="col-xs-12">
        <h2>Neuer Plan</h2>
        <div class="form-inline">
          <div class="form-group">
            <label>Von
              <input type="date" name="from" value="">
            </label>
          </div>
          <div class="form-group">
            <label>Bis
              <input type="date" name="to" value="">
            </label>
          </div>
        </div>
        <button id="create" data-target="@controllers.smt.routes.PlanController.create" class="btn btn-primary" type="button" disabled>Anlegen</button>
      </div>
    </div>

    <div class="row assignees">
      <div class="col-xs-12">
        <h2>Helfer</h2>
        <a href="@controllers.smt.routes.AssigneeController.list">Helfer pflegen</a>
      </div>
    </div>
  </div>
</template>

<style lang="scss">
$icon-font-path: '~bootstrap-sass/assets/fonts/bootstrap/';
@import '~bootstrap-sass/assets/stylesheets/bootstrap';
@import '../assets/settings';

.date-range {
   margin-bottom: 1em;
}
</style>
