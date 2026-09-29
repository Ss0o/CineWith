import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'review-board', component: () => import('../views/ReviewBoardView.vue') },
  { path: '/discover/:section', name: 'movie-discovery', component: () => import('../views/DiscoveryListView.vue'), props: true },
  { path: '/reviews', name: 'review-feed', component: () => import('../views/ReviewFeedView.vue') },
  { path: '/reviews/new', name: 'review-write', component: () => import('../views/ReviewWriteView.vue') },
  { path: '/reviews/:id', name: 'review-detail', component: () => import('../views/ReviewDetailView.vue'), props: true },
  { path: '/movies/:id', name: 'movie-detail', component: () => import('../views/MovieDetailView.vue'), props: true },
  { path: '/mypage', name: 'mypage', component: () => import('../views/MyPageView.vue') },
  { path: '/signup', redirect: '/' },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to) {
    if (to.hash) {
      return { el: to.hash, top: 16, behavior: 'smooth' }
    }
    return { top: 0 }
  },
})
