<script setup>
import { computed, watch, onBeforeUnmount } from 'vue'
import { dataService } from '../data'
import { useRatingStatistics } from '../composables/useRatingStatistics'
import StarRating from './StarRating.vue'

const props = defineProps({ tmdbId: { type: String, required: true } })
const statistics = useRatingStatistics((id) => dataService.movies.ratingStatistics(id))
const { state } = statistics
const buckets = computed(() => Object.entries(state.data?.ratingDistribution ?? {}).sort(([a], [b]) => Number(b) - Number(a)))
const maximumCount = computed(() => Math.max(1, ...buckets.value.map(([, count]) => count)))
const averageLabel = computed(() => state.data?.averageRating == null ? '' : new Intl.NumberFormat('ko-KR', {
  minimumFractionDigits: 1, maximumFractionDigits: 1,
}).format(state.data.averageRating))

watch(() => props.tmdbId, statistics.load, { immediate: true })
onBeforeUnmount(statistics.dispose)
</script>

<template>
  <section class="rating-statistics card" aria-label="Cinewith 평점" :aria-busy="state.loading">
    <h4>Cinewith 평점</h4>
    <p class="meta">Cinewith 회원 리뷰 기준</p>
    <p v-if="state.loading" role="status">평점 통계를 불러오는 중입니다.</p>
    <div v-else-if="state.error" role="alert">
      <p>{{ state.error }}</p>
      <button class="btn btn-secondary" @click="statistics.load(props.tmdbId)">통계 다시 불러오기</button>
    </div>
    <p v-else-if="state.data?.reviewCount === 0" class="meta">아직 등록된 리뷰가 없습니다.</p>
    <template v-else-if="state.data">
      <div class="rating-summary">
        <StarRating v-if="state.data.averageRating !== null" :rating="state.data.averageRating" :show-value="false" size="24px" />
        <strong v-if="state.data.averageRating !== null" :title="`평균 ${state.data.averageRating}점`">{{ averageLabel }} / 5.0</strong>
        <span class="meta">{{ state.data.reviewCount.toLocaleString('ko-KR') }}개의 리뷰</span>
      </div>
      <ul class="rating-distribution" aria-label="평점별 리뷰 수">
        <li v-for="[rating, count] in buckets" :key="rating">
          <span>{{ rating }}</span>
          <progress :value="count" :max="maximumCount" :aria-label="`${rating}점 리뷰 ${count}개`"></progress>
          <span class="bucket-count">{{ count.toLocaleString('ko-KR') }}</span>
        </li>
      </ul>
    </template>
  </section>
</template>

<style scoped>
.rating-statistics { padding: 22px; gap: 12px; }
.rating-statistics h4, .rating-statistics p { margin: 0; }
.rating-summary { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.rating-summary strong { color: var(--color-accent); font-size: 22px; }
.rating-distribution { list-style: none; padding: 0; margin: 4px 0 0; display: grid; gap: 8px; max-width: 560px; }
.rating-distribution li { display: grid; grid-template-columns: 30px minmax(0, 1fr) minmax(32px, auto); gap: 12px; align-items: center; font-size: 12px; font-variant-numeric: tabular-nums; }
.bucket-count { text-align: right; }
progress { width: 100%; height: 10px; accent-color: var(--color-accent); }
</style>
