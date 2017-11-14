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
  props: ['state'],
}
</script>
