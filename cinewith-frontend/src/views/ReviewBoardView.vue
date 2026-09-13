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
const home = ref(null)
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
    if (normalizedQuery) {
      const result = await dataService.movies.search(normalizedQuery)
      if (request === loadVersion) { movies.value = result; home.value = null }
    } else {
      const result = await dataService.movies.discoveryHome()
      if (request === loadVersion) { home.value = result; movies.value = [] }
    }
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
        <h3 style="margin: 0 0 5px">{{ route.query.q ? '영화 검색 결과' : 'CINEWITH' }}</h3>
        <div style="font: 400 12.5px/1.5 var(--font-body); color: color-mix(in srgb, var(--color-text) 48%, transparent)">{{ route.query.q ? '검색한 영화를 확인하세요.' : '지금 볼 영화와 다음 기대작을 찾아보세요.' }}</div>
      </div>
      <div style="display: flex; gap: 8px; max-width: 620px">
        <input class="input" v-model="query" placeholder="영화 제목을 입력하세요" @keyup.enter="search" />
        <button class="btn btn-primary" :disabled="loading" @click="search"><i class="ph ph-magnifying-glass" style="font-size: 15px"></i>{{ loading ? '불러오는 중' : '검색' }}</button>
        <button v-if="route.query.q" class="btn btn-secondary" :disabled="loading" @click="clearSearch">검색 초기화</button>
      </div>
      <div class="fade-rule" style="margin: 0 -26px"></div>
      <div v-if="errorMessage" class="card" style="color: var(--color-danger, #d66)">{{ errorMessage }} <button class="btn btn-secondary" @click="loadMovies(route.query.q)">다시 불러오기</button></div>
      <div v-else-if="loading" style="padding: 42px 0; text-align: center; color: color-mix(in srgb, var(--color-text) 48%, transparent)">영화를 불러오는 중입니다.</div>
      <div v-else-if="route.query.q && !movies.length" style="padding: 42px 0; text-align: center; color: color-mix(in srgb, var(--color-text) 48%, transparent)">검색 결과가 없습니다.</div>
      <div v-else-if="!route.query.q && home" style="display: grid; gap: 30px">
        <template v-for="(section, key) in { nowPlayingRecommendations: home.nowPlayingRecommendations, recommendedMovies: home.recommendedMovies, upcomingRecommendations: home.upcomingRecommendations, ...home.genres }" :key="key">
          <section>
            <h4 style="margin: 0 0 4px">{{ { nowPlayingRecommendations: '🔥 추천 현재 상영작', recommendedMovies: '⭐ 추천 영화', upcomingRecommendations: '🎞 앞으로 나올 기대작', action: '🎬 액션', comedy: '🎬 코미디', romance: '🎬 로맨스', horror: '🎬 공포', sf: '🎬 SF' }[key] }}</h4>
            <p class="meta" style="margin: 0 0 12px">{{ key === 'nowPlayingRecommendations' ? 'TMDB 인기도 기준' : key === 'upcomingRecommendations' ? 'TMDB 인기도와 개봉일 기준' : key === 'recommendedMovies' ? 'TMDB 평점·평가 수·인기도 기준' : '취향별 인기 영화' }}</p>
            <p v-if="section.status === 'UNAVAILABLE'" class="meta">현재 이 섹션의 영화 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.</p>
            <div v-else-if="section.movies.length" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(132px, 1fr)); gap: 12px">
              <RouterLink v-for="movie in section.movies" :key="movie.tmdbId" :to="`/movies/${movie.tmdbId}`" class="card" style="color:var(--color-text); gap:8px">
                <PosterThumb width="100%" height="176px" :label="movie.title" :poster-path="movie.posterPath" />
                <strong style="font-size:13px">{{ movie.title }}</strong><span class="meta">{{ movie.releaseDate || '개봉일 미정' }}</span>
              </RouterLink>
            </div>
            <p v-else class="meta">표시할 영화가 없습니다.</p>
          </section>
        </template>
      </div>
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
