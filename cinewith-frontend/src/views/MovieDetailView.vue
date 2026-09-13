<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import TopNav from '../components/layout/TopNav.vue'
import PosterThumb from '../components/PosterThumb.vue'
import StarRating from '../components/StarRating.vue'
import PaginationControls from '../components/PaginationControls.vue'
import MovieRatingStatistics from '../components/MovieRatingStatistics.vue'
import { usePagedList } from '../composables/usePagedList'
import { dataService } from '../data'
import { tmdbBackdropUrl, tmdbImageUrl } from '../utils/tmdbImage'

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
function profileUrl(profilePath) { return tmdbImageUrl(profilePath, 'w185') }
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
  <TopNav />
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
            <div class="people-grid">
              <article v-for="person in movie.cast" :key="`${person.personId}-${person.character}`" class="person-card">
                <img v-if="profileUrl(person.profilePath)" :src="profileUrl(person.profilePath)" :alt="`${person.name} 프로필`" loading="lazy" @error="$event.target.remove()" />
                <span v-else class="person-placeholder" aria-hidden="true">{{ person.name?.slice(0, 1) || '?' }}</span>
                <strong>{{ person.name }}</strong><span>{{ person.character || '배역 정보 없음' }}</span>
              </article>
            </div>
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
              <RouterLink v-for="review in reviews.state.content" :key="review.id" :to="`/reviews/${review.id}`" class="review-item">
                <div><h3>{{ review.title }}</h3><p class="excerpt">{{ review.excerpt }}</p></div>
                <div class="review-meta"><StarRating :rating="review.rating" /><span>{{ review.author.nickname }} · {{ review.createdAtLabel }}</span></div>
              </RouterLink>
            </template>
            <PaginationControls :page="reviews.state.page" :total-pages="reviews.state.totalPages" :loading="reviews.state.loading" label="리뷰" @change="reviews.load" />
          </section>

          <section class="detail-section similar-section" aria-label="비슷한 영화">
            <div class="section-heading"><h2>비슷한 영화</h2></div>
            <p v-if="recommendationsLoading" role="status">추천 영화를 불러오는 중입니다.</p>
            <div v-else-if="recommendationsError" role="alert"><p>{{ recommendationsError }}</p><button class="btn btn-secondary" @click="loadRecommendations()">추천 다시 불러오기</button></div>
            <p v-else-if="!recommendations.length" class="meta">추천 영화가 없습니다.</p>
            <div v-else class="movie-grid">
              <RouterLink v-for="item in recommendations" :key="item.id" :to="`/movies/${item.id}`" class="similar-card">
                <PosterThumb width="100%" height="190px" :label="item.title" :poster-path="item.posterPath" />
                <strong>{{ item.title }}</strong><span>{{ item.releaseDate?.slice(0, 4) || '개봉일 미정' }}</span>
              </RouterLink>
            </div>
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
.movie-detail { max-width: 1180px; margin: 0 auto; padding: 20px 24px 56px; }.back-button { margin-bottom: 14px; }
.movie-hero { position: relative; isolation: isolate; overflow: hidden; min-height: 330px; padding: 36px; border: 1px solid var(--color-divider); border-radius: var(--radius-lg); background: linear-gradient(125deg, var(--color-accent-900), var(--color-surface)); }.hero-backdrop, .hero-scrim { position: absolute; inset: 0; width: 100%; height: 100%; }.hero-backdrop { object-fit: cover; opacity: .58; z-index: -2; }.hero-scrim { z-index: -1; background: linear-gradient(90deg, rgba(22,24,38,.98) 0%, rgba(22,24,38,.81) 48%, rgba(22,24,38,.48) 100%); }.hero-content { display: flex; align-items: flex-end; gap: 28px; min-height: 258px; }.hero-poster { box-shadow: var(--shadow-lg); }.hero-copy { max-width: 680px; }.eyebrow { margin: 0 0 8px; color: var(--color-accent-300); font-size: 11px; font-weight: 600; letter-spacing: .12em; }.hero-copy h1 { margin: 0; font-size: clamp(31px, 5vw, 48px); }.original-title { margin: 8px 0 0; color: var(--color-neutral-300); font-size: 17px; }.facts { margin: 15px 0 20px; color: var(--color-neutral-200); }.review-action { background: rgba(22,24,38,.55); }
.detail-layout { display: grid; grid-template-columns: minmax(0, 1fr) 290px; gap: 42px; margin-top: 38px; }.detail-main { min-width: 0; }.detail-section { padding: 0 0 34px; margin-bottom: 34px; border-bottom: 1px solid var(--color-divider); }.detail-section h2 { font-size: 21px; margin: 0; }.section-heading { display: flex; align-items: baseline; justify-content: space-between; gap: 14px; margin-bottom: 16px; }.section-heading span, .similar-card span, .crew-list span, .person-card span, .aside-copy, .as-of { color: color-mix(in srgb, var(--color-text) 56%, transparent); font-size: 13px; }.synopsis p { margin: 0; white-space: pre-wrap; color: color-mix(in srgb, var(--color-text) 84%, transparent); line-height: 1.8; }
.people-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }.person-card { display: grid; min-width: 0; gap: 3px; }.person-card img, .person-placeholder { width: 100%; aspect-ratio: .82; margin-bottom: 6px; border-radius: var(--radius-md); background: var(--color-neutral-800); object-fit: cover; }.person-placeholder { display: grid; place-items: center; color: var(--color-neutral-400); font-size: 30px; }.person-card strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; }.person-card span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.crew-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px 24px; padding: 0; margin: 16px 0 0; list-style: none; }.crew-list li { display: flex; justify-content: space-between; gap: 10px; padding-bottom: 8px; border-bottom: 1px solid color-mix(in srgb, var(--color-divider) 70%, transparent); font-size: 14px; }.crew-list span { text-align: right; }
.detail-aside { display: grid; align-content: start; gap: 18px; }.theatrical-info { padding: 20px; border: 1px solid var(--color-divider); border-radius: var(--radius-md); }.theatrical-info p { margin-bottom: 9px; }.theatrical-title { font-weight: 600; }.theatrical-title span { color: var(--color-neutral-400); font-weight: 400; }.aside-copy { line-height: 1.55; }.box-office { color: var(--color-accent-200); font-size: 13px; line-height: 1.7; }.as-of { margin-bottom: 0 !important; font-size: 11px; }
.review-item { display: flex; justify-content: space-between; gap: 20px; padding: 17px 0; border-bottom: 1px solid var(--color-divider); color: var(--color-text); }.review-item h3 { font-size: 16px; }.excerpt { margin: 5px 0 0; color: color-mix(in srgb, var(--color-text) 68%, transparent); white-space: pre-wrap; overflow-wrap: anywhere; }.review-meta { flex: none; display: grid; align-content: start; justify-items: end; gap: 6px; color: color-mix(in srgb, var(--color-text) 52%, transparent); font-size: 12px; }.similar-section { border-bottom: 0; margin-bottom: 0; }.movie-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }.similar-card { display: grid; gap: 6px; min-width: 0; color: var(--color-text); }.similar-card strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; }
@media (max-width: 900px) { .detail-layout { grid-template-columns: 1fr; }.detail-aside { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); }.movie-grid { grid-template-columns: repeat(5, minmax(0, 1fr)); } } @media (max-width: 620px) { .movie-detail { padding: 14px 16px 40px; }.movie-hero { min-height: 0; padding: 20px; }.hero-content { align-items: flex-start; gap: 16px; min-height: 0; }.hero-poster { width: 116px !important; height: 172px !important; }.hero-copy h1 { font-size: 28px; }.original-title { font-size: 14px; }.facts { margin: 10px 0 16px; font-size: 13px; }.detail-layout { gap: 24px; margin-top: 25px; }.detail-aside { grid-template-columns: 1fr; }.people-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }.crew-list { grid-template-columns: 1fr; }.movie-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }.review-item { display: grid; gap: 10px; }.review-meta { justify-items: start; }.detail-section { padding-bottom: 26px; margin-bottom: 26px; } }
</style>
