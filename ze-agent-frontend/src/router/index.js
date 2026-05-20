import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
  },
  {
    path: '/love',
    name: 'love',
    component: () => import('@/views/LoveChatView.vue'),
  },
  {
    path: '/manus',
    name: 'manus',
    component: () => import('@/views/ManusChatView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
