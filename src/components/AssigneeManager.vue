<script>
import Vue from 'vue'
import service from '../api/plan.service'
import StatusIndicator from './StatusIndicator'
import Spinner from 'vue-simple-spinner'

const SERVICES = ['sicherheit', 'mikro', 'tonanlage']

export default {
  data () {
    return {
      assignees: [],
      newName: '',
      saveState: '',
      loading: false
    }
  },
  created () {
    this.load()
  },
  methods: {
    load () {
      this.loading = true
      service.getAssignees().then(as => {
        this.assignees = as.map(a => {
          const services = SERVICES.map(s => a.services.includes(s))
          return {
            ...a,
            services,
            editing: false
          }
        })
      }).finally(_ => { this.loading = false })
    },
    toggleService (assignee, index) {
      Vue.set(assignee.services, index, !assignee.services[index])
    },
    remove (assignee) {
      const index = this.assignees.indexOf(assignee)
      this.assignees.splice(index, 1)
    },
    add () {
      this.assignees.push({
        name: this.newName,
        email: '',
        id: -1,
        services: [false, false, false],
        editing: false
      })
      this.newName = ''
    },
    async save () {
      this.saveState = 'working'
      try {
        const result = await service.saveAssignees(this.serialize())
        this.saveState = result.status === 200 ? 'success' : 'error'
      } catch (e) {
        this.saveState = 'error'
        console.log(e)
      }
    },
    serialize () {
      return [
        SERVICES,
        this.assignees.map(a => [
          a.id,
          a.name,
          a.services.map((v, i) => [v, i]).filter(_ => _[0]).map(_ => _[1]),
          a.email || ''
        ])
      ]
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
            <tr class="helper" v-for="assignee in assignees">
              <td class="name" v-if="assignee.editing"><input type="text" v-model="assignee.name"></td>
              <td class="name" v-else>{{ assignee.name }}</td>
              <td class="email" v-if="assignee.editing"><input type="text" v-model="assignee.email"></td>
              <td class="email" v-else>{{ assignee.email }}</td>
              <td v-for="(service, index) in assignee.services">
                <input type="checkbox" :checked="service" @change="toggleService(assignee, index)">
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
