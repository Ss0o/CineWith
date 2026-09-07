<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import TopNav from '../components/layout/TopNav.vue'
import PosterThumb from '../components/PosterThumb.vue'
import StarRating from '../components/StarRating.vue'
import PaginationControls from '../components/PaginationControls.vue'
import { usePagedList } from '../composables/usePagedList'
import { dataService } from '../data'

const props = defineProps({ id: { type: String, required: true } })
const router = useRouter()
const movie = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const recommendations = ref([])
const recommendationsLoading = ref(false)
const recommendationsError = ref('')
const activeTab = ref('reviews')
const reviews = usePagedList((page, size) => dataService.reviews.listByMovie(props.id, page, size))
let version = 0

async function loadMovie(id, request) {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await dataService.movies.get(id)
    if (request === version) movie.value = result
  } catch (error) {
    if (request === version) errorMessage.value = error.message
  } finally {
    if (request === version) loading.value = false
  }
}
async function loadRecommendations(id = props.id, request = version) {
  recommendationsLoading.value = true
  recommendationsError.value = ''
  try {
    const result = await dataService.movies.recommendations(id)
    if (request === version) recommendations.value = result
  } catch (error) {
    if (request === version) recommendationsError.value = error.message
  } finally {
    if (request === version) recommendationsLoading.value = false
  }
}
watch(() => props.id, (id) => {
  const request = ++version
  movie.value = null
  recommendations.value = []
  activeTab.value = 'reviews'
  reviews.reset()
  loadMovie(id, request)
  reviews.load(0)
  loadRecommendations(id, request)
}, { immediate: true })
onBeforeUnmount(() => { version++; reviews.reset() })
</script>

<template>
  <TopNav />
  <main class="movie-detail">
    <button class="btn btn-ghost" @click="router.back()">← 이전 화면</button>
    <p v-if="loading" role="status">영화를 불러오는 중입니다.</p>
    <div v-else-if="errorMessage" role="alert">
      <p>{{ errorMessage }}</p>
      <button class="btn btn-secondary" @click="loadMovie(props.id, version)">영화 다시 불러오기</button>
    </div>
    <section v-else-if="movie" class="movie-heading">
      <PosterThumb width="150px" height="216px" :label="movie.title" :poster-path="movie.posterPath" />
      <div>
        <h2>{{ movie.title }}</h2>
        <p class="meta">{{ movie.releaseDate || '개봉일 미정' }}</p>
        <RouterLink class="btn btn-primary" :to="`/reviews/new?movieId=${movie.id}`">이 영화 리뷰 쓰기</RouterLink>
      </div>
    </section>
    <div class="seg" role="group" aria-label="영화 정보 선택">
      <button class="btn" :aria-pressed="activeTab === 'reviews'" @click="activeTab = 'reviews'">{{ reviews.state.totalElements === null ? '리뷰' : `리뷰 ${reviews.state.totalElements}` }}</button>
      <button class="btn" :aria-pressed="activeTab === 'similar'" @click="activeTab = 'similar'">비슷한 영화</button>
    </div>
    <section v-if="activeTab === 'reviews'" aria-label="영화 리뷰">
      <p v-if="reviews.state.loading" role="status">리뷰를 불러오는 중입니다.</p>
      <div v-else-if="reviews.state.error" role="alert">
        <p>{{ reviews.state.error }}</p>
        <button class="btn btn-secondary" @click="reviews.load()">리뷰 다시 불러오기</button>
      </div>
      <template v-else>
        <p v-if="!reviews.state.content.length" class="meta">아직 작성된 리뷰가 없습니다.</p>
        <RouterLink v-for="review in reviews.state.content" :key="review.id" :to="`/reviews/${review.id}`" class="review-item">
          <h4>{{ review.title }}</h4>
          <StarRating :rating="review.rating" />
          <p class="excerpt">{{ review.excerpt }}</p>
          <div class="meta">{{ review.author.nickname }} · {{ review.createdAtLabel }}</div>
        </RouterLink>
      </template>
      <PaginationControls :page="reviews.state.page" :total-pages="reviews.state.totalPages" :loading="reviews.state.loading" label="리뷰" @change="reviews.load" />
    </section>
    <section v-else aria-label="비슷한 영화">
      <p v-if="recommendationsLoading" role="status">추천 영화를 불러오는 중입니다.</p>
      <div v-else-if="recommendationsError" role="alert">
        <p>{{ recommendationsError }}</p>
        <button class="btn btn-secondary" @click="loadRecommendations()">추천 다시 불러오기</button>
      </div>
      <p v-else-if="!recommendations.length" class="meta">추천 영화가 없습니다.</p>
      <div v-else class="movie-grid">
        <RouterLink v-for="item in recommendations" :key="item.id" :to="`/movies/${item.id}`" class="card">
          <PosterThumb width="100%" height="190px" :label="item.title" :poster-path="item.posterPath" />
          <span>{{ item.title }}</span><span class="meta">{{ item.releaseDate || '개봉일 미정' }}</span>
        </RouterLink>
      </div>
    </section>
  </main>
</template>

<style scoped>
.movie-detail { max-width: 1000px; margin: auto; padding: 24px; display: grid; gap: 24px; }
.movie-heading { display: flex; align-items: center; gap: 24px; }
.review-item { display: block; padding: 16px 0; border-bottom: 1px solid var(--color-divider); color: var(--color-text); }
.review-item h4 { margin: 0 0 8px; }
.excerpt { white-space: pre-wrap; overflow-wrap: anywhere; }
.seg button[aria-pressed="true"] { color: var(--color-accent); background: var(--color-surface); }
.movie-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px; }
@media (max-width: 540px) { .movie-heading { align-items: flex-start; gap: 16px; } .movie-heading h2 { font-size: 24px; } }
</style>
