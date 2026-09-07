<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import TopNav from '../components/layout/TopNav.vue'
import PosterThumb from '../components/PosterThumb.vue'
import AvatarBadge from '../components/AvatarBadge.vue'
import StarRating from '../components/StarRating.vue'
import { useAuth, AUTH_STATE } from '../composables/useAuth'
import { dataService } from '../data'

const props = defineProps({ id: { type: String, required: true } })

const review = ref(null)
const movie = ref(null)
const comments = ref([])
const loading = ref(true)
const errorMessage = ref('')
const router = useRouter()

const { state } = useAuth()
const isMember = computed(() => state.status === AUTH_STATE.MEMBER)

const newComment = ref('')

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    review.value = await dataService.reviews.get(props.id)
    comments.value = await dataService.comments.list(props.id)
    movie.value = await dataService.movies.get(review.value.movieId)
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

async function createComment() {
  if (!newComment.value.trim()) return
  try {
    comments.value.push(await dataService.comments.create(props.id, newComment.value))
    newComment.value = ''
  } catch (error) { errorMessage.value = error.message }
}

async function editComment(comment) {
  const content = window.prompt('댓글을 수정하세요.', comment.body)
  if (!content?.trim()) return
  try { Object.assign(comment, await dataService.comments.update(comment.id, content)) }
  catch (error) { errorMessage.value = error.message }
}

async function removeComment(comment) {
  if (!window.confirm('댓글을 삭제할까요?')) return
  try { await dataService.comments.delete(comment.id); comments.value = comments.value.filter((item) => item.id !== comment.id) }
  catch (error) { errorMessage.value = error.message }
}

async function removeReview() {
  if (!window.confirm('리뷰를 삭제할까요?')) return
  try { await dataService.reviews.delete(props.id); await router.push('/') }
  catch (error) { errorMessage.value = error.message }
}

async function editReview() {
  const title = window.prompt('리뷰 제목을 수정하세요.', review.value.title)
  if (!title?.trim()) return
  const content = window.prompt('리뷰 내용을 수정하세요.', review.value.excerpt)
  if (!content?.trim()) return
  try { review.value = await dataService.reviews.update(props.id, { title, content }) }
  catch (error) { errorMessage.value = error.message }
}

watch(() => props.id, load, { immediate: true })
</script>

<template>
  <div v-if="loading" style="padding: 60px; text-align: center">불러오는 중...</div>
  <div v-else-if="!review" style="padding: 60px; text-align: center; color: color-mix(in srgb, var(--color-text) 55%, transparent)">
    {{ errorMessage || '존재하지 않는 리뷰입니다.' }}
  </div>
  <template v-else>
    <TopNav />
    <div style="padding: 28px 120px 36px; display: flex; flex-direction: column; gap: 20px; max-width: 960px; margin: 0 auto; width: 100%">
      <div style="display: flex; gap: 16px">
        <PosterThumb width="78px" height="112px" label="포스터" />
        <div style="flex: 1; display: flex; flex-direction: column; gap: 8px">
          <div style="display: flex; align-items: center; gap: 8px">
            <span class="tag tag-neutral">{{ movie?.genre }}</span>
            <RouterLink :to="`/movies/${movie?.id}`" style="font: 400 12.5px/1 var(--font-body)">{{ movie?.title }} · {{ movie?.year }}</RouterLink>
          </div>
          <h3 style="margin: 0; letter-spacing: -0.02em">{{ review.title }}</h3>
          <div style="display: flex; align-items: center; gap: 14px">
            <StarRating :rating="review.rating" size="14px" />
            <span style="font: 400 11.5px/1 var(--font-body); color: color-mix(in srgb, var(--color-text) 45%, transparent)">
              {{ review.createdAtDate }}
            </span>
          </div>
          <div style="display: flex; align-items: center; gap: 10px; margin-top: 2px">
            <AvatarBadge :initial="review.author.initial" size="26px" font-size="11px" />
            <div>
              <div style="font: 400 12.5px/1.2 var(--font-body)">{{ review.author.nickname }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="fade-rule"></div>

      <div style="font: 400 15px/1.9 var(--font-body); color: color-mix(in srgb, var(--color-text) 88%, transparent); text-wrap: pretty; display: flex; flex-direction: column; gap: 16px">
        <template v-for="(block, i) in review.body" :key="i">
          <p v-if="block.type === 'p'" style="margin: 0">{{ block.text }}</p>
          <p
            v-else-if="block.type === 'quote'"
            style="margin: 0; padding-left: 16px; border-left: 2px solid var(--color-accent); color: color-mix(in srgb, var(--color-text) 70%, transparent); font-size: 14px"
          >
            {{ block.text }}
          </p>
        </template>
      </div>

      <div v-if="review.tags.length" style="display: flex; gap: 6px">
        <span v-for="tag in review.tags" :key="tag" class="tag tag-outline">#{{ tag }}</span>
      </div>

      <div style="display: flex; align-items: center; gap: 8px; padding: 14px 0">
        <div style="flex: 1"></div>
        <button v-if="isMember" class="btn btn-ghost" @click="editReview">리뷰 수정</button>
        <button v-if="isMember" class="btn btn-ghost" @click="removeReview">리뷰 삭제</button>
      </div>

      <div class="fade-rule"></div>

      <div style="display: flex; align-items: baseline; gap: 10px">
        <h5 style="margin: 0">댓글 {{ comments.length }}</h5>
      </div>

      <div v-if="isMember" style="display: flex; gap: 10px">
        <AvatarBadge :initial="state.member?.initial ?? '?'" size="30px" font-size="12px" />
        <div style="flex: 1; display: flex; flex-direction: column; gap: 8px">
          <textarea class="input" v-model="newComment" placeholder="이 리뷰에 대한 생각을 남겨주세요" style="min-height: 64px"></textarea>
          <div style="display: flex; align-items: center; gap: 10px">
            <div style="flex: 1"></div>
            <button class="btn btn-primary" @click="createComment">댓글 등록</button>
          </div>
        </div>
      </div>
      <div v-else style="padding: 14px; border-radius: var(--radius-md); border: 1px dashed var(--color-divider); text-align: center; font-size: 13px; color: color-mix(in srgb, var(--color-text) 55%, transparent)">
        로그인한 회원만 댓글을 작성할 수 있습니다.
      </div>

      <div style="display: flex; flex-direction: column; gap: 2px">
        <template v-for="(comment, index) in comments" :key="comment.id">
          <div style="display: flex; gap: 10px; padding: 14px 0">
            <AvatarBadge :initial="comment.author.initial" size="30px" font-size="12px" />
            <div style="flex: 1">
              <div class="meta" style="margin-bottom: 5px">
                <span style="color: var(--color-text)">{{ comment.author.nickname }}</span>
                <span v-if="comment.isReviewAuthor" class="tag tag-accent" style="font-size: 9px; padding: 1px 6px">작성자</span>
                <span>{{ comment.createdAtLabel }}</span>
              </div>
              <div style="font: 400 13.5px/1.7 var(--font-body)">{{ comment.body }}</div>
              <div v-if="comment.upvotes" class="meta" style="margin-top: 7px; gap: 12px">
                <span style="display: flex; align-items: center; gap: 5px"><i class="ph ph-arrow-up" style="font-size: 12px"></i>{{ comment.upvotes }}</span>
                <span>답글</span>
              </div>
              <div v-if="isMember" class="meta" style="margin-top: 7px">
                <button class="btn btn-ghost" style="font-size: 11px" @click="editComment(comment)">수정</button>
                <button class="btn btn-ghost" style="font-size: 11px" @click="removeComment(comment)">삭제</button>
              </div>
            </div>
          </div>
          <div class="fade-rule" v-if="index < comments.length - 1"></div>
        </template>
      </div>
      <div v-if="errorMessage" style="color: var(--color-danger, #d66); font-size: 12px">{{ errorMessage }}</div>
    </div>
  </template>
</template>
