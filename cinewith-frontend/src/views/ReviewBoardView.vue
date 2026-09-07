<script setup>
import { ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import BoardShell from '../components/layout/BoardShell.vue'
import PosterThumb from '../components/PosterThumb.vue'
import { dataService } from '../data'

const route = useRoute()
const router = useRouter()
const query = ref('')
const movies = ref([])
const loading = ref(false)
const errorMessage = ref('')
let loadVersion = 0

async function search() {
  const normalizedQuery = query.value.trim()
  await router.push(normalizedQuery ? { path: '/', query: { q: normalizedQuery } } : { path: '/' })
}

async function clearSearch() {
  query.value = ''
  await search()
}

async function loadMovies(routeQuery) {
  const normalizedQuery = typeof routeQuery === 'string' ? routeQuery.trim() : ''
  query.value = normalizedQuery
  const request = ++loadVersion
  loading.value = true
  errorMessage.value = ''
  try {
    const result = normalizedQuery
      ? await dataService.movies.search(normalizedQuery)
      : await dataService.movies.nowPlaying()
    if (request === loadVersion) movies.value = result
  } catch (error) {
    if (request !== loadVersion) return
    movies.value = []
    errorMessage.value = error.message
  } finally {
    if (request === loadVersion) loading.value = false
  }
}

watch(() => route.query.q, loadMovies, { immediate: true })
</script>

<template>
  <BoardShell has-right-rail>
    <div style="flex: 1; min-width: 0; padding: 24px 26px; display: flex; flex-direction: column; gap: 18px">
      <div>
        <h3 style="margin: 0 0 5px">{{ route.query.q ? '영화 검색 결과' : '최근 상영작' }}</h3>
        <div style="font: 400 12.5px/1.5 var(--font-body); color: color-mix(in srgb, var(--color-text) 48%, transparent)">{{ route.query.q ? '검색한 영화의 리뷰를 확인하세요.' : '현재 상영 중인 영화를 만나보세요.' }}</div>
      </div>
      <div style="display: flex; gap: 8px; max-width: 620px">
        <input class="input" v-model="query" placeholder="영화 제목을 입력하세요" @keyup.enter="search" />
        <button class="btn btn-primary" :disabled="loading" @click="search"><i class="ph ph-magnifying-glass" style="font-size: 15px"></i>{{ loading ? '불러오는 중' : '검색' }}</button>
        <button v-if="route.query.q" class="btn btn-secondary" :disabled="loading" @click="clearSearch">검색 초기화</button>
      </div>
      <div class="fade-rule" style="margin: 0 -26px"></div>
      <div v-if="errorMessage" class="card" style="color: var(--color-danger, #d66)">{{ errorMessage }} <button class="btn btn-secondary" @click="loadMovies(route.query.q)">다시 불러오기</button></div>
      <div v-else-if="loading" style="padding: 42px 0; text-align: center; color: color-mix(in srgb, var(--color-text) 48%, transparent)">영화를 불러오는 중입니다.</div>
      <div v-else-if="!movies.length" style="padding: 42px 0; text-align: center; color: color-mix(in srgb, var(--color-text) 48%, transparent)">{{ route.query.q ? '검색 결과가 없습니다.' : '현재 표시할 상영작이 없습니다.' }}</div>
      <div v-else style="display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 14px">
        <RouterLink v-for="movie in movies" :key="movie.id" :to="`/movies/${movie.id}`" class="card elev-sm" style="color: var(--color-text); gap: 10px">
          <PosterThumb width="100%" height="220px" :label="movie.title" :poster-path="movie.posterPath" />
          <div style="font: 500 15px/1.35 var(--font-heading)">{{ movie.title }}</div>
          <div class="meta">{{ movie.releaseDate || '개봉일 미정' }}</div>
          <span class="btn btn-secondary" style="justify-content: center">리뷰 보기</span>
        </RouterLink>
      </div>
    </div>
    <template #right-rail>
      <div class="h6" style="margin-bottom: 12px">이용 방법</div>
      <div style="display: flex; flex-direction: column; gap: 12px; font: 400 12.5px/1.65 var(--font-body); color: color-mix(in srgb, var(--color-text) 62%, transparent)">
        <div><span style="color: var(--color-accent)">01</span> TMDB 영화 제목을 검색합니다.</div>
        <div><span style="color: var(--color-accent)">02</span> 영화 상세에서 최신 리뷰를 확인합니다.</div>
        <div><span style="color: var(--color-accent)">03</span> 로그인 후 리뷰와 댓글을 작성합니다.</div>
      </div>
      <div class="fade-rule" style="margin: 24px -20px"></div>
      <RouterLink to="/reviews/new" class="btn btn-primary" style="justify-content: center">리뷰 작성하기</RouterLink>
    </template>
  </BoardShell>
</template>
