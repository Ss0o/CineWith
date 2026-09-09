<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BoardShell from '../components/layout/BoardShell.vue'
import ReviewListItem from '../components/ReviewListItem.vue'
import PaginationControls from '../components/PaginationControls.vue'
import { usePagedList } from '../composables/usePagedList'
import { dataService } from '../data'

const route = useRoute()
const router = useRouter()
const input = ref('')
const activeQuery = ref('')
const reviews = usePagedList((page, size) => dataService.reviews.listFeed(activeQuery.value, page, size))

function normalizedQuery(value) {
  return typeof value === 'string' ? value.trim() : ''
}

async function search() {
  const query = normalizedQuery(input.value)
  if (query === activeQuery.value) {
    reviews.load(0)
    return
  }
  await router.push(query ? { path: '/reviews', query: { q: query } } : { path: '/reviews' })
}

watch(() => route.query.q, (query) => {
  activeQuery.value = normalizedQuery(query)
  input.value = activeQuery.value
  reviews.reset()
  reviews.load(0)
}, { immediate: true })

onBeforeUnmount(reviews.reset)
</script>

<template>
  <BoardShell has-right-rail>
    <section style="flex: 1; min-width: 0; padding: 24px 26px; display: flex; flex-direction: column; gap: 18px">
      <div>
        <h3 style="margin: 0 0 5px">{{ activeQuery ? '리뷰 검색 결과' : '전체 리뷰' }}</h3>
        <p class="meta" style="margin: 0">Cinewith 회원이 작성한 최신 리뷰를 확인하세요.</p>
      </div>
      <form style="display: flex; gap: 8px; max-width: 620px" @submit.prevent="search">
        <input v-model="input" class="input" maxlength="100" placeholder="리뷰, 영화 제목을 검색하세요" aria-label="리뷰 검색" />
        <button class="btn btn-primary" :disabled="reviews.state.loading">검색</button>
        <button v-if="activeQuery" type="button" class="btn btn-secondary" :disabled="reviews.state.loading" @click="router.push('/reviews')">검색 초기화</button>
      </form>
      <div class="fade-rule" style="margin: 0 -26px"></div>
      <p v-if="reviews.state.loading" role="status" style="padding: 42px 0; text-align: center">리뷰를 불러오는 중입니다.</p>
      <div v-else-if="reviews.state.error" class="card" role="alert">
        <p>{{ reviews.state.error }}</p>
        <button class="btn btn-secondary" @click="reviews.load()">다시 불러오기</button>
      </div>
      <template v-else>
        <p v-if="!reviews.state.content.length" style="padding: 42px 0; text-align: center" class="meta">
          {{ activeQuery ? '검색 결과가 없습니다.' : '아직 등록된 리뷰가 없습니다.' }}
        </p>
        <div v-else>
          <p class="meta">총 {{ reviews.state.totalElements.toLocaleString('ko-KR') }}개의 리뷰</p>
          <ReviewListItem v-for="review in reviews.state.content" :key="review.id" :review="review" />
        </div>
      </template>
      <PaginationControls :page="reviews.state.page" :total-pages="reviews.state.totalPages" :loading="reviews.state.loading" label="전체 리뷰" @change="reviews.load" />
    </section>
    <template #right-rail>
      <div class="h6" style="margin-bottom: 12px">전체 리뷰</div>
      <p style="font: 400 12.5px/1.65 var(--font-body); color: color-mix(in srgb, var(--color-text) 62%, transparent)">영화 제목, 리뷰 제목, 본문으로 리뷰를 찾을 수 있습니다.</p>
    </template>
  </BoardShell>
</template>
