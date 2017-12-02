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
      <div class="labeled-control">
        <div><label for="name">Name</label></div>
        <div><input type="text" name="name" v-model="name"></div>
      </div>
      <div class="labeled-control">
        <div><label>Passwort</label></div>
        <div><input type="password" name="password" v-model="password"></div>
      </div>
      <button type="submit" @click.prevent="login">Login</button>
    </form>
  </div>
</template>

<style lang="scss" scoped>
@import '../assets/style';

.labeled-control {
  margin-bottom: 1rem;
}
</style>
