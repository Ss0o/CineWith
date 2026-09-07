<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TopNav from '../components/layout/TopNav.vue'
import StarRating from '../components/StarRating.vue'
import PosterThumb from '../components/PosterThumb.vue'
import { dataService } from '../data'

const title = ref('')
const content = ref('')
const rating = ref(4.5)
const selectedMovie = ref(null) // set once movie search (MOVIE-001) is wired to a real API
const query = ref('')
const searchResults = ref([])
const errorMessage = ref('')
const router = useRouter()
const route = useRoute()

onMounted(async () => {
  if (!route.query.movieId) return
  try { selectedMovie.value = await dataService.movies.get(route.query.movieId) }
  catch (error) { errorMessage.value = error.message }
})

async function searchMovies() {
  if (!query.value.trim()) return
  try { searchResults.value = await dataService.movies.search(query.value) }
  catch (error) { errorMessage.value = error.message }
}

async function submitReview() {
  if (!selectedMovie.value) { errorMessage.value = '영화를 선택해주세요.'; return }
  try {
    const review = await dataService.reviews.create({ tmdbId: selectedMovie.value.id, title: title.value, content: content.value, rating: rating.value })
    await router.push(`/reviews/${review.id}`)
  } catch (error) { errorMessage.value = error.message }
}
</script>

<template>
  <TopNav :show-board-link="false">
    <template #actions>
      <button class="btn btn-primary" @click="submitReview">등록</button>
    </template>
  </TopNav>

  <div style="padding: 26px 32px 32px; display: flex; gap: 26px; max-width: 860px; margin: 0 auto; width: 100%">
    <div style="flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 18px">
      <h4 style="margin: 0">리뷰 쓰기</h4>

      <div class="field">
        <label>영화 선택</label>
        <div v-if="!selectedMovie" style="display: flex; gap: 8px">
          <input class="input" v-model="query" placeholder="영화 제목 검색" @keyup.enter="searchMovies" />
          <button class="btn btn-secondary" @click="searchMovies">검색</button>
        </div>
        <div v-if="!selectedMovie && searchResults.length" class="card" style="margin-top: 8px">
          <button v-for="movie in searchResults" :key="movie.id" class="btn btn-ghost" @click="selectedMovie = movie">{{ movie.title }} · {{ movie.year || '개봉일 미정' }}</button>
        </div>
        <div v-if="selectedMovie" style="display: flex; align-items: center; gap: 12px; padding: 10px 12px; border: 1px solid var(--color-accent); border-radius: var(--radius-md); background: var(--color-surface)">
          <PosterThumb width="34px" height="48px" />
          <div style="flex: 1">
            <div style="font: 400 13px/1.3 var(--font-body)">{{ selectedMovie.title }}</div>
            <div style="font: 400 11px/1.4 var(--font-body); color: color-mix(in srgb, var(--color-text) 45%, transparent)">{{ selectedMovie.year || '개봉일 미정' }}</div>
          </div>
          <i class="ph ph-x" style="font-size: 14px; color: color-mix(in srgb, var(--color-text) 45%, transparent); cursor: pointer" @click="selectedMovie = null"></i>
        </div>
      </div>
      <div v-if="errorMessage" style="color: var(--color-danger, #d66); font-size: 12px">{{ errorMessage }}</div>

      <div class="field">
        <label>평점</label>
        <div style="display: flex; align-items: center; gap: 10px">
          <StarRating :rating="rating" size="26px" :show-value="false" />
          <span style="font: 400 13px/1 ui-monospace, monospace; color: color-mix(in srgb, var(--color-text) 60%, transparent)">{{ rating.toFixed(1) }} / 5.0</span>
        </div>
        <input v-model.number="rating" type="range" min="0.5" max="5" step="0.5" style="width: 240px; accent-color: var(--color-accent)" />
      </div>

      <div class="field">
        <label>제목</label>
        <input class="input" v-model="title" placeholder="리뷰 제목을 입력하세요" />
      </div>

      <div class="field">
        <label>본문</label>
        <textarea class="input" v-model="content" style="min-height: 220px" placeholder="영화에 대한 생각을 자유롭게 남겨주세요"></textarea>
      </div>
    </div>

    <div style="width: 236px; flex: none; display: flex; flex-direction: column; gap: 16px">
      <div style="padding: 12px; border-radius: var(--radius-md); border: 1px solid var(--color-divider); font: 400 11.5px/1.7 var(--font-body); color: color-mix(in srgb, var(--color-text) 50%, transparent)">
        평점은 0.5점부터 5.0점까지 0.5점 단위로 저장됩니다. 영화, 제목, 본문과 평점은 모두 실제 Backend API로 전송됩니다.
      </div>
    </div>
  </div>
</template>
