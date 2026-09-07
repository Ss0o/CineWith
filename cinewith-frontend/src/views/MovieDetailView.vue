<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useRouter } from 'vue-router'
import TopNav from '../components/layout/TopNav.vue'
import PosterThumb from '../components/PosterThumb.vue'
import AvatarBadge from '../components/AvatarBadge.vue'
import StarRating from '../components/StarRating.vue'
import { dataService } from '../data'

const props = defineProps({ id: { type: String, required: true } })
const router = useRouter()

const movie = ref(null)
const movieReviews = ref([])
const recommendations = ref([])
const loading = ref(true)
const errorMessage = ref('')
const activeTab = ref('reviews')

const distribution = computed(() => movie.value?.ratingDistribution ?? [])
const maxBar = computed(() => Math.max(...distribution.value, 1))

watch(() => props.id, async () => {
  loading.value = true
  errorMessage.value = ''
  try { [movie.value, movieReviews.value, recommendations.value] = await Promise.all([dataService.movies.get(props.id), dataService.reviews.listByMovie(props.id), dataService.movies.recommendations(props.id)]) }
  catch (error) { errorMessage.value = error.message }
  finally { loading.value = false }
}, { immediate: true })
</script>

<template>
  <div v-if="loading" style="padding: 60px; text-align: center">불러오는 중...</div>
  <div v-else-if="!movie" style="padding: 60px; text-align: center; color: color-mix(in srgb, var(--color-text) 55%, transparent)">
    {{ errorMessage || '존재하지 않는 영화입니다.' }}
  </div>
  <template v-else>
    <TopNav />
    <div style="padding: 16px 32px 0">
      <button class="btn btn-ghost" type="button" @click="router.back()"><i class="ph ph-arrow-left"></i> 이전 화면</button>
    </div>
    <div style="padding: 26px 32px; display: flex; gap: 26px; background: linear-gradient(160deg, var(--color-surface), var(--color-bg) 70%)">
      <PosterThumb width="150px" height="216px" :label="movie.title" :poster-path="movie.posterPath" />
      <div style="flex: 1; display: flex; flex-direction: column; gap: 12px">
        <div>
          <div class="h6" style="margin-bottom: 8px">{{ movie.year }} · {{ movie.genre }} · {{ movie.runtimeMinutes }}분 · {{ movie.rating12 }}</div>
          <h2 style="margin: 0 0 4px">{{ movie.title }}</h2>
          <div v-if="movie.director" style="font: 400 13px/1 var(--font-body); color: color-mix(in srgb, var(--color-text) 50%, transparent)">
            감독 {{ movie.director }}<template v-if="movie.cast.length"> · 출연 {{ movie.cast.join(', ') }}</template>
          </div>
        </div>
        <div style="display: flex; align-items: flex-end; gap: 28px; padding: 6px 0">
          <div>
            <div style="font: 500 34px/1 var(--font-heading); color: var(--color-accent)">{{ movie.avgRating.toFixed(1) }}</div>
            <div style="font: 400 11px/1.6 var(--font-body); color: color-mix(in srgb, var(--color-text) 45%, transparent)">리뷰 {{ movie.reviewCount }}개 평균</div>
          </div>
          <div v-if="distribution.length" style="display: flex; align-items: flex-end; gap: 5px; height: 52px">
            <div
              v-for="(value, i) in distribution"
              :key="i"
              :style="{
                width: '14px',
                height: Math.round((value / maxBar) * 100) + '%',
                background: i === distribution.length - 1 ? 'var(--color-accent)' : i === distribution.length - 2 ? 'var(--color-accent-700)' : i >= distribution.length - 4 ? 'var(--color-neutral-700)' : 'var(--color-neutral-800)',
                borderRadius: '2px 2px 0 0',
              }"
            ></div>
          </div>
          <div v-if="distribution.length" style="font: 400 11px/1.6 var(--font-body); color: color-mix(in srgb, var(--color-text) 45%, transparent)">1점 → 5점 분포</div>
        </div>
        <div v-if="movie.synopsis" style="font: 400 13.5px/1.8 var(--font-body); color: color-mix(in srgb, var(--color-text) 72%, transparent); max-width: 640px; text-wrap: pretty">
          {{ movie.synopsis }}
        </div>
        <div style="display: flex; gap: 8px; margin-top: 2px">
          <RouterLink class="btn btn-primary" :to="`/reviews/new?movieId=${movie.id}`"><i class="ph ph-pencil-simple" style="font-size: 15px"></i>이 영화 리뷰 쓰기</RouterLink>
        </div>
      </div>
    </div>
    <div class="fade-rule"></div>
    <div style="display: flex; gap: 26px; padding: 22px 32px 32px">
      <div style="flex: 1; min-width: 0">
        <div class="seg" style="margin-bottom: 18px">
          <label class="seg-opt"><input type="radio" name="mtab" value="reviews" v-model="activeTab" />리뷰 {{ movieReviews.length }}</label>
          <label class="seg-opt"><input type="radio" name="mtab" value="oneline" v-model="activeTab" />한줄평</label>
          <label class="seg-opt"><input type="radio" name="mtab" value="similar" v-model="activeTab" />비슷한 영화</label>
        </div>
        <div v-if="activeTab === 'reviews'" style="display: flex; flex-direction: column; gap: 2px">
          <template v-for="(review, i) in movieReviews" :key="review.id">
            <RouterLink :to="`/reviews/${review.id}`" style="padding: 14px 0; display: flex; flex-direction: column; gap: 7px; color: var(--color-text)">
              <div style="display: flex; align-items: baseline; gap: 10px">
                <span style="font: 500 15.5px/1.3 var(--font-heading)">{{ review.title }}</span>
                <StarRating :rating="review.rating" size="12px" />
              </div>
              <div style="font: 400 13px/1.7 var(--font-body); color: color-mix(in srgb, var(--color-text) 66%, transparent)">{{ review.excerpt }}</div>
              <div class="meta">
                <AvatarBadge :initial="review.author.initial" size="18px" font-size="9px" />{{ review.author.nickname }}
              </div>
            </RouterLink>
            <div class="fade-rule" v-if="i < movieReviews.length - 1"></div>
          </template>
          <div v-if="!movieReviews.length" style="padding: 24px 0; color: color-mix(in srgb, var(--color-text) 50%, transparent); font-size: 13px">
            아직 작성된 리뷰가 없습니다.
          </div>
        </div>
        <div v-else-if="activeTab === 'similar'" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px">
          <RouterLink v-for="item in recommendations" :key="item.id" :to="`/movies/${item.id}`" class="card" style="color: var(--color-text)">
            <PosterThumb width="100%" height="190px" :label="item.title" :poster-path="item.posterPath" />
            <span>{{ item.title }}</span><span class="meta">{{ item.releaseDate || '개봉일 미정' }}</span>
          </RouterLink>
          <div v-if="!recommendations.length" class="meta">추천 영화가 없습니다.</div>
        </div>
        <div v-else style="padding: 24px 0; color: color-mix(in srgb, var(--color-text) 50%, transparent); font-size: 13px">한줄평 API는 현재 제공되지 않습니다.</div>
      </div>
    </div>
  </template>
</template>
