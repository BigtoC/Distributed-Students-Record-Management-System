import User from './pages/User.vue'
import Index from './pages/Index.vue'
import Error from './pages/404.vue'

const routes = [
  { path: '/', name: 'index', component: Index },
  { path: '/user/:port', name: 'user', component: User },
  { path: '/404', name: 'error', component: Error },
]

export default routes;