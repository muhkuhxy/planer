<script>
import Smt from './components/Smt'
import AppNav from './components/AppNav'

export default {
  components: { Smt, AppNav }
}
</script>

<template>
  <div>
    <AppNav></AppNav>
    <router-view></router-view>
  </div>
</template>

<style lang="scss">
$icon-font-path: '~bootstrap-sass/assets/fonts/bootstrap/';
@import '~bootstrap-sass/assets/stylesheets/bootstrap';
@import './assets/style'
</style>
