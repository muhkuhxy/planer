<template>
  <div class="working" v-if="state == 'working'">
    <span class="glyphicon glyphicon-repeat"></span>
  </div>
  <div class="save-ok" v-else-if="state == 'success'">
    <span class="glyphicon glyphicon-ok-circle"></span>
  </div>
  <div class="error" v-else-if="state == 'error'">
    <span class="glyphicon glyphicon-remove-circle"></span>
  </div>
</template>

<script>
export default {
  name: 'status-indicator',
  props: ['state']
}
</script>

<style lang="scss" scoped>
@import '../assets/style';

.glyphicon {
  font-size: 1.5em;
  margin-top: 0.25em;
}

.save-ok {
  display: inline-block;
  color: $brand-success;
}

.error {
  display: inline-block;
  color: red;
}

.working {
  display: inline-block;
  animation: 2s linear 0s infinite normal turnit;
}

@keyframes turnit {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}
</style>
