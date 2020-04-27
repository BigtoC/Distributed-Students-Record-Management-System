<template>
<div id="main">
  <p>
    <img src="../assets/logo.png" width="360">
  </p>
  <el-select v-model="value" @change="handleChange"
  clearable placeholder="Choose which user to login" id="select">
    <el-option-group
      v-for="group in options"
      :key="group.label"
      :label="group.label">
      <el-option
        v-for="item in group.options"
        :key="item.value"
        :label="item.label"
        :value="item.value">
      </el-option>
    </el-option-group>
  </el-select>
  &nbsp;&nbsp;&nbsp;
  <el-button type="primary" plain @click="go">Go</el-button>
</div>
</template>

<script>
// import routes from './routes'

export default {
  name: 'Index',
  data() {
    return {
      selected: '',
      options: [{
          label: 'Teacher',
          options: [{
            value: '50005',
            label: 'Alice'
          }]
        }, {
          label: 'Students',
          options: [{
            value: '50006',
          label: 'Bob'
          }, {
            value: '50007',
          label: 'Cindy'
          }, {
            value: '50008',
          label: 'Dolores'
          }]
        }],
        value: ''
      }
  },
  methods: {
    handleChange(params) {
        this.selected = params;
        console.log(this.selected)
    },
    go() {
        const path = '/user/:port';
        if (this.$route.path !== path) {
          this.$router.push({
              name: 'user',
              path: path,
              params: {port: this.selected}
          })
      }
    },
    goBack() {
      window.history.length > 1 ? this.$router.go(-1) : this.$router.push('/')
    },
  },
}
</script>

<style scoped>
  #main {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  align-items: center;
  color: #2c3e50;
  margin-top: 60px;
}
</style>