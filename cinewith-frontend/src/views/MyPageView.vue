<script setup>
import { computed, onMounted } from 'vue'
import TopNav from '../components/layout/TopNav.vue'
import AvatarBadge from '../components/AvatarBadge.vue'
import { useAuth, AUTH_STATE } from '../composables/useAuth'
import { dataService } from '../data'
import { usePagedList } from '../composables/usePagedList'
import PaginationControls from '../components/PaginationControls.vue'

const { state, openLoginModal } = useAuth()
const isMember = computed(() => state.status === AUTH_STATE.MEMBER)
const reviews = usePagedList((page, size) => dataService.reviews.listMine(page, size))
const comments = usePagedList((page, size) => dataService.comments.listMine(page, size))

onMounted(() => {
  if (isMember.value) {
    reviews.load()
    comments.load()
  }
})
</script>

<template>
  <TopNav />
  <div v-if="!isMember" style="padding: 80px 32px; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 14px; color: color-mix(in srgb, var(--color-text) 55%, transparent)">
    <div>로그인 후 회원 정보를 확인할 수 있습니다.</div>
    <button class="btn btn-primary" @click="openLoginModal">Google로 로그인</button>
  </div>
  <div v-else style="padding: 30px 32px; max-width: 760px; margin: 0 auto; display: flex; flex-direction: column; gap: 20px">
    <h3 style="margin: 0">내 회원 정보</h3>
    <div class="card elev-sm" style="padding: 22px; flex-direction: row; align-items: center; gap: 16px">
      <AvatarBadge :initial="state.member.initial" size="58px" font-size="22px" />
      <div style="display: flex; flex-direction: column; gap: 7px">
        <div style="font: 500 20px/1.2 var(--font-heading)">{{ state.member.nickname }}</div>
        <div class="meta">{{ state.member.email || '이메일 정보 없음' }}</div>
        <div class="meta">{{ state.member.provider }} 로그인 · 가입일 {{ state.member.joinedAt }}</div>
      </div>
    </div>
    <section class="card" style="padding: 20px">
      <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 14px">
        <h3 style="margin: 0">내가 쓴 리뷰</h3>
        <span v-if="reviews.state.totalElements !== null" class="meta">{{ reviews.state.totalElements }}개</span>
      </div>
      <div v-if="reviews.state.loading" class="meta">리뷰를 불러오는 중입니다.</div>
      <div v-else-if="reviews.state.error" style="display: flex; gap: 10px; align-items: center" class="meta">
        <span>{{ reviews.state.error }}</span><button class="btn btn-secondary" @click="reviews.load(reviews.state.page)">다시 시도</button>
      </div>
      <div v-else-if="reviews.state.content.length === 0" class="meta">아직 작성한 리뷰가 없습니다.</div>
      <div v-else style="display: flex; flex-direction: column; gap: 12px">
        <RouterLink v-for="review in reviews.state.content" :key="review.id" :to="`/reviews/${review.id}`" style="color: inherit; text-decoration: none; padding: 12px 0; border-bottom: 1px solid var(--color-divider)">
          <div class="meta">{{ review.movieTitle }}</div>
          <strong>{{ review.title }}</strong>
          <div class="meta" style="margin-top: 5px">{{ review.rating }}점 · {{ review.createdAtLabel }}</div>
        </RouterLink>
      </div>
      <PaginationControls :page="reviews.state.page" :total-pages="reviews.state.totalPages" :loading="reviews.state.loading" label="내 리뷰" @change="reviews.load" />
    </section>

    <section class="card" style="padding: 20px">
      <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 14px">
        <h3 style="margin: 0">내가 쓴 댓글</h3>
        <span v-if="comments.state.totalElements !== null" class="meta">{{ comments.state.totalElements }}개</span>
      </div>
      <div v-if="comments.state.loading" class="meta">댓글을 불러오는 중입니다.</div>
      <div v-else-if="comments.state.error" style="display: flex; gap: 10px; align-items: center" class="meta">
        <span>{{ comments.state.error }}</span><button class="btn btn-secondary" @click="comments.load(comments.state.page)">다시 시도</button>
      </div>
      <div v-else-if="comments.state.content.length === 0" class="meta">아직 작성한 댓글이 없습니다.</div>
      <div v-else style="display: flex; flex-direction: column; gap: 12px">
        <RouterLink v-for="comment in comments.state.content" :key="comment.id" :to="`/reviews/${comment.reviewId}`" style="color: inherit; text-decoration: none; padding: 12px 0; border-bottom: 1px solid var(--color-divider)">
          <div class="meta">{{ comment.movieTitle }} · {{ comment.reviewTitle }}</div>
          <div style="margin-top: 5px">{{ comment.body }}</div>
          <div class="meta" style="margin-top: 5px">{{ comment.createdAtLabel }}</div>
        </RouterLink>
      </div>
      <PaginationControls :page="comments.state.page" :total-pages="comments.state.totalPages" :loading="comments.state.loading" label="내 댓글" @change="comments.load" />
    </section>
  </div>
</template>
