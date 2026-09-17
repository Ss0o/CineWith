<script setup>
import { nextTick, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import BoardShell from '../components/layout/BoardShell.vue'
import MovieCarousel from '../components/MovieCarousel.vue'
import PosterThumb from '../components/PosterThumb.vue'
import { dataService } from '../data'

const route = useRoute()
const router = useRouter()
const movies = ref([])
const home = ref(null)
const loading = ref(false)
const errorMessage = ref('')
let loadVersion = 0

const sectionMeta = {
  nowPlayingRecommendations: { id: 'now-playing', title: '🔥 추천 현재 상영작', description: 'TMDB 인기도 기준' },
  recommendedMovies: { id: 'recommended-movies', title: '⭐ 추천 영화', description: 'TMDB 평점·평가 수·인기도 기준' },
  upcomingRecommendations: { id: 'upcoming-movies', title: '🎞 앞으로 나올 기대작', description: 'TMDB 인기도와 개봉일 기준' },
  action: { id: 'genre-action', title: '🎬 액션', description: '취향별 인기 영화' },
  adventure: { id: 'genre-adventure', title: '🎬 모험', description: '취향별 인기 영화' },
  animation: { id: 'genre-animation', title: '🎬 애니메이션', description: '취향별 인기 영화' },
  comedy: { id: 'genre-comedy', title: '🎬 코미디', description: '취향별 인기 영화' },
  drama: { id: 'genre-drama', title: '🎬 드라마', description: '취향별 인기 영화' },
  fantasy: { id: 'genre-fantasy', title: '🎬 판타지', description: '취향별 인기 영화' },
  horror: { id: 'genre-horror', title: '🎬 공포', description: '취향별 인기 영화' },
  romance: { id: 'genre-romance', title: '🎬 로맨스', description: '취향별 인기 영화' },
  sf: { id: 'genre-sf', title: '🎬 SF', description: '취향별 인기 영화' },
  thriller: { id: 'genre-thriller', title: '🎬 스릴러', description: '취향별 인기 영화' },
}

async function clearSearch() {
  await router.push({ path: '/' })
}

async function loadMovies(routeQuery) {
  const normalizedQuery = typeof routeQuery === 'string' ? routeQuery.trim() : ''
  const request = ++loadVersion
  loading.value = true
  errorMessage.value = ''
  try {
    if (normalizedQuery) {
      const result = await dataService.movies.search(normalizedQuery)
      if (request === loadVersion) { movies.value = result; home.value = null }
    } else {
      const result = await dataService.movies.discoveryHome()
      if (request === loadVersion) {
        home.value = result
        movies.value = []
        await nextTick()
        if (route.hash) document.querySelector(route.hash)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
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
  <BoardShell>
    <div style="flex: 1; min-width: 0; padding: 24px 26px; display: flex; flex-direction: column; gap: 18px">
      <div v-if="route.query.q" style="display: flex; align-items: center; justify-content: space-between; gap: 12px">
        <div>
          <h3 style="margin: 0 0 5px">영화 검색 결과</h3>
          <div style="font: 400 12.5px/1.5 var(--font-body); color: color-mix(in srgb, var(--color-text) 48%, transparent)">검색한 영화를 확인하세요.</div>
        </div>
        <button class="btn btn-secondary" :disabled="loading" @click="clearSearch">검색 초기화</button>
      </div>
      <div v-if="errorMessage" class="card" style="color: var(--color-danger, #d66)">{{ errorMessage }} <button class="btn btn-secondary" @click="loadMovies(route.query.q)">다시 불러오기</button></div>
      <div v-else-if="loading" style="padding: 42px 0; text-align: center; color: color-mix(in srgb, var(--color-text) 48%, transparent)">영화를 불러오는 중입니다.</div>
      <div v-else-if="route.query.q && !movies.length" style="padding: 42px 0; text-align: center; color: color-mix(in srgb, var(--color-text) 48%, transparent)">검색 결과가 없습니다.</div>
      <div v-else-if="!route.query.q && home" style="display: grid; gap: 30px">
        <template v-for="(section, key) in { nowPlayingRecommendations: home.nowPlayingRecommendations, recommendedMovies: home.recommendedMovies, upcomingRecommendations: home.upcomingRecommendations }" :key="key">
          <section :id="sectionMeta[key].id" style="scroll-margin-top: 16px">
            <h4 style="margin: 0 0 4px">{{ sectionMeta[key].title }}</h4>
            <p class="meta" style="margin: 0 0 12px">{{ sectionMeta[key].description }}</p>
            <p v-if="section.status === 'UNAVAILABLE'" class="meta">현재 이 섹션의 영화 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.</p>
            <MovieCarousel v-else-if="section.movies.length" :movies="section.movies" :label="sectionMeta[key].title" />
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
  </BoardShell>
</template>
