import Vue from 'vue'
import VueRouter from 'vue-router'
import axios from 'axios'
import routes from './routes'

import App from './App.vue'
import './plugins/element.js'


axios.defaults.headers.post['Content-Type'] = "application/json";
Vue.prototype.$axios = axios;
Vue.config.productionTip = false
Vue.use(VueRouter)


const router = new VueRouter({
  mode: 'history',
  routes: routes
})

new Vue({
  router,
  render: h => h(App),
}).$mount('#app')
