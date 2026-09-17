<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import AppHeader from '../components/layout/AppHeader.vue'
import MovieCarousel from '../components/MovieCarousel.vue'
import PersonCarousel from '../components/PersonCarousel.vue'
import PosterThumb from '../components/PosterThumb.vue'
import ReviewCarousel from '../components/ReviewCarousel.vue'
import PaginationControls from '../components/PaginationControls.vue'
import MovieRatingStatistics from '../components/MovieRatingStatistics.vue'
import { usePagedList } from '../composables/usePagedList'
import { dataService } from '../data'
import { tmdbBackdropUrl } from '../utils/tmdbImage'

const props = defineProps({ id: { type: String, required: true } })
const router = useRouter()
const movie = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const recommendations = ref([])
const recommendationsLoading = ref(false)
const recommendationsError = ref('')
const theatrical = ref(null)
const theatricalLoading = ref(false)
const theatricalError = ref('')
const reviews = usePagedList((page, size) => dataService.reviews.listByMovie(props.id, page, size))
const backdropUrl = computed(() => tmdbBackdropUrl(movie.value?.backdropPath))
const movieFacts = computed(() => {
  if (!movie.value) return []
  return [movie.value.releaseDate?.slice(0, 4), movie.value.genres.join(' · '), movie.value.productionCountries.join(', '),
    movie.value.runtimeMinutes ? `${movie.value.runtimeMinutes}분` : null].filter(Boolean)
})
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
async function loadTheatrical(id = props.id, request = version) {
  theatricalLoading.value = true
  theatricalError.value = ''
  try {
    const result = await dataService.movies.koreanTheatrical(id)
    if (request === version) theatrical.value = result
  } catch (error) {
    if (request === version) theatricalError.value = error.message
  } finally {
    if (request === version) theatricalLoading.value = false
  }
}
function audienceLabel(value) {
  const audience = Number(value)
  if (!Number.isFinite(audience)) return ''
  if (audience >= 10000) return `${(audience / 10000).toLocaleString('ko-KR', { maximumFractionDigits: 1 })}만 명`
  return `${audience.toLocaleString('ko-KR')}명`
}
watch(() => props.id, (id) => {
  const request = ++version
  movie.value = null
  recommendations.value = []
  theatrical.value = null
  reviews.reset()
  loadMovie(id, request)
  reviews.load(0)
  loadRecommendations(id, request)
  loadTheatrical(id, request)
}, { immediate: true })
onBeforeUnmount(() => { version++; reviews.reset() })
</script>

<template>
  <AppHeader />
  <main class="movie-detail">
    <button class="btn btn-ghost back-button" @click="router.back()">← 이전 화면</button>
    <p v-if="loading" role="status">영화를 불러오는 중입니다.</p>
    <div v-else-if="errorMessage" role="alert">
      <p>{{ errorMessage }}</p>
      <button class="btn btn-secondary" @click="loadMovie(props.id, version)">영화 다시 불러오기</button>
    </div>
    <template v-else-if="movie">
      <section class="movie-hero">
        <img v-if="backdropUrl" class="hero-backdrop" :src="backdropUrl" alt="" aria-hidden="true" @error="$event.target.remove()" />
        <div class="hero-scrim"></div>
        <div class="hero-content">
          <PosterThumb class="hero-poster" width="178px" height="258px" :label="movie.title" :poster-path="movie.posterPath" />
          <div class="hero-copy">
            <p class="eyebrow">MOVIE</p>
            <h1>{{ movie.title }}</h1>
            <p v-if="movie.originalTitle && movie.originalTitle !== movie.title" class="original-title">{{ movie.originalTitle }}</p>
            <p v-if="movieFacts.length" class="facts">{{ movieFacts.join(' · ') }}</p>
            <RouterLink class="btn btn-primary review-action" :to="`/reviews/new?movieId=${movie.id}`">이 영화 리뷰 쓰기</RouterLink>
          </div>
        </div>
      </section>

      <div class="detail-layout">
        <div class="detail-main">
          <section v-if="movie.overview" class="detail-section synopsis">
            <h2>줄거리</h2><p>{{ movie.overview }}</p>
          </section>
          <section v-if="movie.cast.length" class="detail-section">
            <div class="section-heading"><h2>출연</h2><span>{{ movie.cast.length }}명</span></div>
            <PersonCarousel :people="movie.cast" />
          </section>
          <section v-if="movie.crew.length" class="detail-section">
            <h2>제작진</h2>
            <ul class="crew-list"><li v-for="person in movie.crew" :key="`${person.personId}-${person.job}`"><strong>{{ person.name }}</strong><span>{{ person.job || person.department }}</span></li></ul>
          </section>

          <section class="detail-section reviews-section" aria-label="영화 리뷰">
            <div class="section-heading"><h2>리뷰</h2><span v-if="reviews.state.totalElements !== null">{{ reviews.state.totalElements }}개</span></div>
            <p v-if="reviews.state.loading" role="status">리뷰를 불러오는 중입니다.</p>
            <div v-else-if="reviews.state.error" role="alert"><p>{{ reviews.state.error }}</p><button class="btn btn-secondary" @click="reviews.load()">리뷰 다시 불러오기</button></div>
            <template v-else>
              <p v-if="!reviews.state.content.length" class="meta">아직 작성된 리뷰가 없습니다.</p>
              <ReviewCarousel v-else :reviews="reviews.state.content" />
            </template>
            <PaginationControls :page="reviews.state.page" :total-pages="reviews.state.totalPages" :loading="reviews.state.loading" label="리뷰" @change="reviews.load" />
          </section>

          <section class="detail-section similar-section" aria-label="비슷한 영화">
            <div class="section-heading"><h2>비슷한 영화</h2></div>
            <p v-if="recommendationsLoading" role="status">추천 영화를 불러오는 중입니다.</p>
            <div v-else-if="recommendationsError" role="alert"><p>{{ recommendationsError }}</p><button class="btn btn-secondary" @click="loadRecommendations()">추천 다시 불러오기</button></div>
            <p v-else-if="!recommendations.length" class="meta">추천 영화가 없습니다.</p>
            <MovieCarousel v-else :movies="recommendations" label="비슷한 영화" />
          </section>
        </div>

        <aside class="detail-aside">
          <MovieRatingStatistics :tmdb-id="props.id" />
          <section class="theatrical-info" aria-label="국내 극장 정보">
            <p class="eyebrow">KOFIC 국내 극장 정보</p>
            <p v-if="theatricalLoading" class="meta">국내 극장 정보를 불러오는 중입니다.</p>
            <div v-else-if="theatricalError" role="alert"><p class="meta">국내 극장 정보를 불러오지 못했습니다.</p><button class="btn btn-secondary" @click="loadTheatrical()">다시 불러오기</button></div>
            <template v-else-if="theatrical?.status === 'AVAILABLE'">
              <p class="theatrical-title">{{ theatrical.title }} <span v-if="theatrical.titleEnglish">{{ theatrical.titleEnglish }}</span></p>
              <p v-if="theatrical.genres.length || theatrical.nations.length" class="aside-copy">{{ theatrical.genres.join(' / ') }}<template v-if="theatrical.nations.length"> · {{ theatrical.nations.join(', ') }}</template></p>
              <p v-if="theatrical.runningTimeMinutes || theatrical.watchGrade" class="aside-copy"><template v-if="theatrical.runningTimeMinutes">{{ theatrical.runningTimeMinutes }}분</template><template v-if="theatrical.watchGrade"> · {{ theatrical.watchGrade }}</template></p>
              <p v-if="theatrical.boxOfficeRank || theatrical.daysSinceRelease || theatrical.accumulatedAudience" class="box-office">
                <template v-if="theatrical.boxOfficeRank">전일 박스오피스 {{ theatrical.boxOfficeRank }}위<template v-if="theatrical.salesShare !== null"> ({{ theatrical.salesShare }}%)</template></template>
                <template v-if="theatrical.daysSinceRelease"> · 개봉 {{ theatrical.daysSinceRelease }}일째</template>
                <template v-if="theatrical.accumulatedAudience"> · 누적 관객 {{ audienceLabel(theatrical.accumulatedAudience) }}</template>
              </p>
              <p v-if="theatrical.asOfDate" class="as-of">기준일 {{ theatrical.asOfDate }}</p>
            </template>
            <p v-else-if="theatrical?.status === 'NOT_AVAILABLE'" class="meta">국내 극장 정보가 등록되지 않은 영화입니다.</p>
            <p v-else class="meta">현재 국내 극장 정보를 사용할 수 없습니다.</p>
          </section>
        </aside>
      </div>
    </template>
  </main>
</template>

<style scoped>
.movie-detail { max-width: 1180px; margin: 0 auto; padding: 20px 24px 64px; }.back-button { margin-bottom: 14px; }
.movie-hero { position: relative; isolation: isolate; overflow: hidden; width: 100vw; min-height: 520px; margin-left: calc(50% - 50vw); padding: 66px max(24px, calc((100vw - 1120px) / 2)); background: linear-gradient(125deg, var(--color-accent-900), var(--color-surface)); }.hero-backdrop, .hero-scrim { position: absolute; inset: 0; width: 100%; height: 100%; }.hero-backdrop { object-fit: cover; object-position: center; opacity: .84; z-index: -2; }.hero-scrim { z-index: -1; background: linear-gradient(90deg, rgba(12,14,24,.98) 0%, rgba(12,14,24,.9) 35%, rgba(12,14,24,.56) 67%, rgba(12,14,24,.3) 100%); }.hero-content { display: flex; align-items: flex-end; gap: 34px; min-height: 388px; }.hero-poster { box-shadow: 0 16px 44px rgba(0, 0, 0, .65); }.hero-copy { max-width: 720px; text-shadow: 0 2px 18px rgba(0, 0, 0, .72); }.eyebrow { margin: 0 0 10px; color: #d8d2ff; font-size: 13px; font-weight: 700; letter-spacing: .14em; }.hero-copy h1 { margin: 0; color: #fff; font-size: clamp(42px, 5vw, 64px); font-weight: 700; }.original-title { margin: 10px 0 0; color: #f1efff; font-size: 21px; }.facts { margin: 18px 0 24px; color: #fff; font-size: 18px; font-weight: 500; }.review-action { padding: 12px 18px; color: #fff; border-color: #d6d0ff; background: rgba(20, 20, 34, .7); }
.detail-layout { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 48px; margin-top: 46px; }.detail-main { min-width: 0; }.detail-section { padding: 0 0 38px; margin-bottom: 38px; border-bottom: 1px solid var(--color-divider); }.detail-section h2 { color: #fff; font-size: 27px; font-weight: 650; margin: 0; }.section-heading { display: flex; align-items: baseline; justify-content: space-between; gap: 14px; margin-bottom: 20px; }.section-heading span, .similar-card span, .crew-list span, .person-card span, .aside-copy, .as-of { color: var(--color-neutral-300); font-size: 15px; }.synopsis p { margin: 0; white-space: pre-wrap; color: #f3f4fa; font-size: 17px; line-height: 1.9; }
.crew-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px 28px; padding: 0; margin: 20px 0 0; list-style: none; }.crew-list li { display: flex; justify-content: space-between; gap: 10px; padding-bottom: 10px; border-bottom: 1px solid color-mix(in srgb, var(--color-divider) 70%, transparent); color: #fff; font-size: 16px; }.crew-list span { text-align: right; }
.detail-aside { display: grid; align-content: start; gap: 20px; }.theatrical-info { padding: 24px; border: 1px solid color-mix(in srgb, var(--color-divider) 150%, transparent); border-radius: var(--radius-md); background: color-mix(in srgb, var(--color-surface) 85%, #000); }.theatrical-info p { margin-bottom: 11px; }.theatrical-title { color: #fff; font-size: 17px; font-weight: 650; }.theatrical-title span { color: var(--color-neutral-200); font-weight: 400; }.aside-copy { color: var(--color-neutral-200); font-size: 15px; line-height: 1.65; }.box-office { color: #e4dfff; font-size: 15px; line-height: 1.75; }.as-of { margin-bottom: 0 !important; font-size: 13px; }
.review-item { display: flex; justify-content: space-between; gap: 20px; padding: 20px 0; border-bottom: 1px solid var(--color-divider); color: var(--color-text); }.review-item h3 { color: #fff; font-size: 18px; }.excerpt { margin: 7px 0 0; color: var(--color-neutral-200); font-size: 15px; white-space: pre-wrap; overflow-wrap: anywhere; }.review-meta { flex: none; display: grid; align-content: start; justify-items: end; gap: 7px; color: var(--color-neutral-300); font-size: 14px; }.similar-section { border-bottom: 0; margin-bottom: 0; }.movie-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }.similar-card { display: grid; gap: 7px; min-width: 0; color: var(--color-text); }.similar-card strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #fff; font-size: 16px; }
:deep(.rating-statistics) { padding: 24px; border: 1px solid color-mix(in srgb, var(--color-divider) 150%, transparent); background: color-mix(in srgb, var(--color-surface) 85%, #000); }:deep(.rating-statistics h4) { color: #fff; font-size: 22px; }:deep(.rating-statistics .meta) { color: var(--color-neutral-300); font-size: 14px; }:deep(.rating-summary strong) { font-size: 24px; }
/* The hero stays dark for its backdrop image; the detail content follows the light application theme. */
.detail-section h2, .person-card strong, .crew-list li, .theatrical-title, .review-item h3, .similar-card strong { color: var(--color-text); }
.section-heading span, .similar-card span, .crew-list span, .person-card span, .aside-copy, .as-of, .review-meta { color: var(--color-neutral-600); }
.synopsis p { color: var(--color-text); }
.person-card img, .person-placeholder { background: var(--color-neutral-200); }
.person-placeholder { color: var(--color-neutral-600); }
.theatrical-info, :deep(.rating-statistics) { border-color: var(--color-divider); background: var(--color-surface); }
.theatrical-title span, .aside-copy { color: var(--color-neutral-600); }
.box-office { color: var(--color-accent-700); }
.excerpt { color: var(--color-neutral-600); }
:deep(.rating-statistics h4) { color: var(--color-text); }
:deep(.rating-statistics .meta) { color: var(--color-neutral-600); }
@media (max-width: 900px) { .movie-hero { min-height: 460px; padding: 52px 24px; }.detail-layout { grid-template-columns: 1fr; }.detail-aside { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); }.movie-grid { grid-template-columns: repeat(5, minmax(0, 1fr)); } } @media (max-width: 620px) { .movie-detail { padding: 14px 16px 44px; }.movie-hero { min-height: 390px; padding: 34px 20px; }.hero-scrim { background: linear-gradient(90deg, rgba(12,14,24,.96), rgba(12,14,24,.7)); }.hero-content { align-items: flex-end; gap: 16px; min-height: 0; }.hero-poster { width: 126px !important; height: 186px !important; }.hero-copy h1 { font-size: 34px; }.original-title { font-size: 16px; }.facts { margin: 11px 0 17px; font-size: 15px; }.detail-layout { gap: 26px; margin-top: 30px; }.detail-aside { grid-template-columns: 1fr; }.crew-list { grid-template-columns: 1fr; }.movie-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }.review-item { display: grid; gap: 10px; }.review-meta { justify-items: start; }.detail-section { padding-bottom: 28px; margin-bottom: 28px; }.detail-section h2 { font-size: 24px; }.synopsis p { font-size: 16px; } }
</style>
