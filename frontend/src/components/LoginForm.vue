<script>
import {req} from '../lib/common'

export default {
  data () {
    return {
      name: '',
      password: ''
    }
  },
  methods: {
    login () {
      req.post('/api/login', {
        name: this.name,
        password: this.password
      }).then(r => {
        this.$store.commit('login', r.responseText)
        this.$router.push('plan')
      })
    }
  }
}
</script>

<template>
  <div class="container">
    <form>
      <div>
        <label>Name <input type="text" name="name" v-model="name"></label>
      </div>
      <div>
        <label>Passwort <input type="password" name="password" v-model="password"></label>
      </div>
      <button type="submit" @click.prevent="login">Login</button>
    </form>
  </div>
</template>

<style lang="scss">
$icon-font-path: '~bootstrap-sass/assets/fonts/bootstrap/';
@import '~bootstrap-sass/assets/stylesheets/bootstrap';
@import '../assets/settings';
</style>
