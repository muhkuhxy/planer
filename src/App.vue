<script>
import AppNav from './components/AppNav'

export default {
  components: { AppNav }
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
@import './assets/style';
@import './assets/print';
@import './assets/animations';
</style>
