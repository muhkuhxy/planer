<script>
import Vue from 'vue'
import service from '../api/plan.service'
import StatusIndicator from './StatusIndicator'
import Spinner from 'vue-simple-spinner'
import { mapState, mapGetters } from 'vuex'
import cloneDeep from 'lodash/cloneDeep'

export default {
  data () {
    return {
      newName: '',
      saveState: '',
      loading: false,
      assigneeVm: []
    }
  },
  created () {
    this.load()
  },
  computed: {
    ...mapState(['assignees', 'services']),
    ...mapGetters(['servicesByName'])
  },
  methods: {
    async load () {
      this.loading = true
      try {
        await this.$store.dispatch('loadAssignees')
        this.assigneeVm = cloneDeep(this.assignees)
        this.assigneeVm.forEach(_ => Vue.set(_, 'editing', false))
      } finally {
        this.loading = false
      }
    },
    hasService (assignee, serviceId) {
      return assignee.services.includes(serviceId)
    },
    toggleService (assignee, serviceId) {
      const index = assignee.services.indexOf(serviceId)
      if (index < 0) {
        assignee.services.push(serviceId)
      } else {
        assignee.services.splice(index, 1)
      }
    },
    remove (assignee) {
      const index = this.assigneeVm.indexOf(assignee)
      this.assigneeVm.splice(index, 1)
    },
    add () {
      this.assigneeVm.push({
        name: this.newName,
        email: '',
        id: -1,
        services: [],
        editing: false
      })
      this.newName = ''
    },
    async save () {
      this.saveState = 'working'
      try {
        const result = await service.saveAssignees(this.assigneeVm)
        this.saveState = result.status === 200 ? 'success' : 'error'
      } catch (e) {
        this.saveState = 'error'
        // eslint-disable-next-line no-console
        console.error(e)
      }
    }
  },
  components: {
    StatusIndicator,
    Spinner
  }
}
</script>

<template>
  <div class="container">

    <header class="row">
      <div class="col-xs-12">
        <h2>Freiwillige</h2>
      </div>
    </header>

    <div class="row">
      <Spinner v-if="loading"></Spinner>
      <div v-else class="col-xs-12">
        <table class="table table-condensed">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Sicherheit</th>
              <th>Mikro</th>
              <th>Tonanlage</th>
              <th class="text-center"></th>
              <th class="text-right"></th>
            </tr>
          </thead>
          <tbody>
            <tr class="helper" v-for="assignee in assigneeVm" :key="assignee.id">
              <td class="name" v-if="assignee.editing"><input type="text" v-model="assignee.name"></td>
              <td class="name" v-else>{{ assignee.name }}</td>
              <td class="email" v-if="assignee.editing"><input type="text" v-model="assignee.email"></td>
              <td class="email" v-else>{{ assignee.email }}</td>
              <td>
                <input type="checkbox" :checked="hasService(assignee, servicesByName['sicherheit'].id)" @change="toggleService(assignee, servicesByName['sicherheit'].id)">
              </td>
              <td>
                <input type="checkbox" :checked="hasService(assignee, servicesByName['mikro'].id)" @change="toggleService(assignee, servicesByName['mikro'].id)">
              </td>
              <td>
                <input type="checkbox" :checked="hasService(assignee, servicesByName['tonanlage'].id)" @change="toggleService(assignee, servicesByName['tonanlage'].id)">
              </td>
              <td>
                <button class="btn btn-xs btn-default" type="button" @click="assignee.editing = !assignee.editing">
                  <i class="glyphicon" :class="{'glyphicon-pencil': !assignee.editing, 'glyphicon-ok': assignee.editing}"></i>
                </button>
              </td>
              <td>
                <button class="btn btn-xs btn-danger" type="button" @click="remove(assignee)">
                  <i class="glyphicon glyphicon-trash"></i>
                </button>
              </td>
            </tr>
          </tbody>
          <tfoot>
            <tr>
              <td class="add-helper">
                <label>Hinzufügen <input type="text" v-model="newName"></label>
                <button class="btn btn-sm btn-default" @click="add"><span class="glyphicon glyphicon-plus-sign"></span></button></td>
              <td></td>
              <td></td>
              <td></td>
              <td></td>
              <td></td>
              <td></td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>

    <div class="row save">
      <div class="col-xs-12 text-center">
        <button class="btn btn-primary" type="button" @click="save" :disabled="this.saveState === 'working'">Speichern</button>
        <status-indicator :state="saveState"></status-indicator>
      </div>
    </div>

  </div>
</template>

<style lang="scss">
.save {
  margin-bottom: 2rem;
}
</style>
