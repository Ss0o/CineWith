<script setup>
import { computed, watch } from 'vue'
import { RouterLink } from 'vue-router'
import BoardShell from '../components/layout/BoardShell.vue'
import PaginationControls from '../components/PaginationControls.vue'
import PosterThumb from '../components/PosterThumb.vue'
import { usePagedList } from '../composables/usePagedList'
import { dataService } from '../data'

const props = defineProps({ section: { type: String, required: true } })

const sectionMeta = {
  recommended: { title: '추천 영화', description: 'TMDB 평점·평가 수·인기도 기준' },
  'now-playing': { title: '현재 상영작', description: 'TMDB 인기도 기준' },
  upcoming: { title: '개봉 예정작', description: 'TMDB 인기도와 개봉일 기준' },
  action: { title: '액션 영화', description: 'TMDB 인기도 기준' }, adventure: { title: '모험 영화', description: 'TMDB 인기도 기준' },
  animation: { title: '애니메이션 영화', description: 'TMDB 인기도 기준' }, comedy: { title: '코미디 영화', description: 'TMDB 인기도 기준' },
  drama: { title: '드라마 영화', description: 'TMDB 인기도 기준' }, fantasy: { title: '판타지 영화', description: 'TMDB 인기도 기준' },
  horror: { title: '공포 영화', description: 'TMDB 인기도 기준' }, romance: { title: '로맨스 영화', description: 'TMDB 인기도 기준' },
  sf: { title: 'SF 영화', description: 'TMDB 인기도 기준' }, thriller: { title: '스릴러 영화', description: 'TMDB 인기도 기준' },
}
const meta = computed(() => sectionMeta[props.section] ?? { title: '영화 탐색', description: '' })
const movies = usePagedList((page) => dataService.movies.discoveryPage(props.section, page))

watch(() => props.section, () => { movies.reset(); movies.load(0) }, { immediate: true })
</script>

<template>
  <BoardShell>
    <main style="padding: 24px 26px 48px">
      <h3 style="margin: 0 0 5px">{{ meta.title }}</h3>
      <p class="meta" style="margin: 0 0 24px">{{ meta.description }}</p>
      <p v-if="movies.state.loading" role="status">영화를 불러오는 중입니다.</p>
      <div v-else-if="movies.state.error" role="alert"><p>{{ movies.state.error }}</p><button class="btn btn-secondary" @click="movies.load()">다시 불러오기</button></div>
      <p v-else-if="!movies.state.content.length" class="meta">표시할 영화가 없습니다.</p>
      <div v-else style="display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 14px">
        <RouterLink v-for="movie in movies.state.content" :key="movie.id" :to="`/movies/${movie.id}`" class="card elev-sm" style="color: var(--color-text); gap: 10px">
          <PosterThumb width="100%" height="220px" :label="movie.title" :poster-path="movie.posterPath" />
          <strong>{{ movie.title }}</strong>
          <span class="meta">{{ movie.releaseDate || '개봉일 미정' }}</span>
        </RouterLink>
      </div>
      <PaginationControls :page="movies.state.page" :total-pages="movies.state.totalPages" :loading="movies.state.loading" label="영화" @change="movies.load" />
    </main>
  </BoardShell>
</template>
