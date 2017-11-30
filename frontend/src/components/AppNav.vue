<template>
  <nav class="navbar navbar-default">
    <ul class="nav navbar-nav">
      <li><router-link to="/plan">SMT-Plan</router-link></li>
    </ul>
    <p class="state">
      <span v-if="user">
        Eingeloggt als {{ user }} -
        <a @click.prevent="logout" href="#" class="navbar-link">Logout</a>
      </span>
      <span v-else>
        Nicht eingeloggt -
        <router-link to="/login">Login</router-link>
      </span>
    </p>
  </nav>
</template>

<script>
import { mapState } from 'vuex'
import { req } from '../lib/common'
import { handleUnauthorized } from '../lib/appHelpers'

export default {
  computed: mapState(['user']),
  methods: {
    logout () {
      req.get('/api/logout').then(r => {
        this.$store.commit('logout')
        this.$router.push('/login')
      })
    }
  },
  created () {
    req.get('/api/user').then(r => {
      this.$store.commit('login', r.responseText)
    }, handleUnauthorized.bind(this))
  }
}
</script>

<style lang="scss">
$icon-font-path: '~bootstrap-sass/assets/fonts/bootstrap/';
@import '~bootstrap-sass/assets/stylesheets/bootstrap';
@import '../assets/settings';

.state {
  float: right;
  color: #777;
  margin: 15px;
}
</style>
