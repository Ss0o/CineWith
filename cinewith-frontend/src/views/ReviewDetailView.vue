<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import TopNav from '../components/layout/TopNav.vue'
import PosterThumb from '../components/PosterThumb.vue'
import AvatarBadge from '../components/AvatarBadge.vue'
import StarRating from '../components/StarRating.vue'
import PaginationControls from '../components/PaginationControls.vue'
import { useAuth, AUTH_STATE } from '../composables/useAuth'
import { usePagedList } from '../composables/usePagedList'
import { isOwnContent } from '../utils/ownership'
import { dataService } from '../data'

const props = defineProps({ id: { type: String, required: true } })
const router = useRouter()
const { state } = useAuth()
const isMember = computed(() => state.status === AUTH_STATE.MEMBER)
const review = ref(null)
const movie = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const actionError = ref('')
const busy = ref(false)
const editing = ref(false)
const draft = ref({ title: '', content: '', rating: 0.5 })
const newComment = ref('')
const editingCommentId = ref(null)
const commentDraft = ref('')
const ownsReview = computed(() => isOwnContent(state, review.value))
const comments = usePagedList((page, size) => dataService.comments.list(props.id, page, size))
let version = 0

async function load() {
  const request = ++version
  const id = props.id
  review.value = null
  movie.value = null
  loading.value = true
  errorMessage.value = ''
  actionError.value = ''
  editing.value = false
  editingCommentId.value = null
  newComment.value = ''
  busy.value = false
  comments.reset()
  try {
    const result = await dataService.reviews.get(id)
    if (request !== version) return
    review.value = result
    comments.load(0)
    // The stored review remains readable even if TMDB is unavailable.
    dataService.movies.get(result.movieId).then((value) => {
      if (request === version) movie.value = value
    }).catch(() => {})
  } catch (error) {
    if (request === version) errorMessage.value = error.message
  } finally {
    if (request === version) loading.value = false
  }
}

async function mutate(action) {
  if (busy.value) return
  const request = version
  busy.value = true
  actionError.value = ''
  try { await action(() => request === version) }
  catch (error) { if (request === version) actionError.value = error.message }
  finally { if (request === version) busy.value = false }
}
function editReview() {
  draft.value = { title: review.value.title, content: review.value.excerpt, rating: review.value.rating }
  editing.value = true
}
async function saveReview() {
  if (!ownsReview.value) return
  await mutate(async (current) => {
    const result = await dataService.reviews.update(props.id, { ...draft.value })
    if (current()) { review.value = result; editing.value = false }
  })
}
async function removeReview() {
  if (!ownsReview.value || !window.confirm('리뷰와 소속 댓글을 삭제할까요?')) return
  await mutate(async (current) => {
    const movieId = review.value.movieId
    await dataService.reviews.delete(props.id)
    if (current()) await router.push(`/movies/${movieId}`)
  })
}
async function createComment() {
  if (!isMember.value || !newComment.value.trim()) return
  await mutate(async (current) => {
    await dataService.comments.create(props.id, newComment.value)
    if (!current()) return
    newComment.value = ''
    // Refresh server totals, then show the last page (oldest-first ordering).
    await comments.load()
    if (current() && !comments.state.error && comments.state.page < comments.state.totalPages - 1) {
      await comments.load(comments.state.totalPages - 1)
    }
  })
}
function editComment(comment) {
  editingCommentId.value = comment.id
  commentDraft.value = comment.body
}
async function saveComment(comment) {
  if (!isOwnContent(state, comment)) return
  await mutate(async (current) => {
    const result = await dataService.comments.update(comment.id, commentDraft.value)
    if (current()) { Object.assign(comment, result); editingCommentId.value = null }
  })
}
async function removeComment(comment) {
  if (!isOwnContent(state, comment) || !window.confirm('댓글을 삭제할까요?')) return
  await mutate(async (current) => {
    await dataService.comments.delete(comment.id)
    if (current()) await comments.load()
  })
}
function changeCommentPage(page) {
  editingCommentId.value = null
  comments.load(page)
}
watch(() => props.id, load, { immediate: true })
watch(isMember, () => { editing.value = false; editingCommentId.value = null })
onBeforeUnmount(() => { version++; comments.reset() })
</script>

<template>
  <TopNav />
  <main class="review-detail">
    <p v-if="loading" role="status">리뷰를 불러오는 중입니다.</p>
    <div v-else-if="!review" role="alert">
      <p>{{ errorMessage || '존재하지 않는 리뷰입니다.' }}</p>
      <button class="btn btn-secondary" @click="load">다시 불러오기</button>
    </div>
    <template v-else>
      <div class="review-heading">
        <PosterThumb width="78px" height="112px" :label="review.movieTitle" :poster-path="movie?.posterPath" />
        <div>
          <RouterLink :to="`/movies/${review.movieId}`">{{ review.movieTitle }}</RouterLink>
          <h3>{{ review.title }}</h3>
          <StarRating :rating="review.rating" />
          <div class="meta">{{ review.author.nickname }} · {{ review.createdAtDate }}</div>
        </div>
      </div>
      <form v-if="editing && ownsReview" class="editor" @submit.prevent="saveReview">
        <label for="review-title">제목</label>
        <input id="review-title" v-model="draft.title" class="input" required />
        <label for="review-content">본문</label>
        <textarea id="review-content" v-model="draft.content" class="input" rows="8" required></textarea>
        <label for="review-rating">평점 {{ Number(draft.rating).toFixed(1) }} / 5.0</label>
        <input id="review-rating" v-model.number="draft.rating" type="range" min="0.5" max="5" step="0.5" />
        <div class="actions">
          <button class="btn btn-primary" :disabled="busy || !draft.title.trim() || !draft.content.trim()">저장</button>
          <button type="button" class="btn btn-secondary" :disabled="busy" @click="editing = false">취소</button>
        </div>
      </form>
      <p v-else class="review-body">{{ review.excerpt }}</p>
      <div v-if="ownsReview && !editing" class="actions">
        <button class="btn btn-ghost" :disabled="busy" @click="editReview">리뷰 수정</button>
        <button class="btn btn-ghost" :disabled="busy" @click="removeReview">리뷰 삭제</button>
      </div>
      <p v-if="actionError" role="alert" class="error">{{ actionError }}</p>
      <div class="fade-rule"></div>
      <h5>{{ comments.state.totalElements === null ? '댓글' : `댓글 ${comments.state.totalElements}` }}</h5>
      <form v-if="isMember" class="editor" @submit.prevent="createComment">
        <label for="new-comment">댓글 작성</label>
        <textarea id="new-comment" v-model="newComment" class="input" placeholder="이 리뷰에 대한 생각을 남겨주세요" required></textarea>
        <button class="btn btn-primary" :disabled="busy || comments.state.loading || !newComment.trim()">댓글 등록</button>
      </form>
      <p v-else class="meta">로그인한 회원만 댓글을 작성할 수 있습니다.</p>
      <p v-if="comments.state.loading" role="status">댓글을 불러오는 중입니다.</p>
      <div v-else-if="comments.state.error" role="alert">
        <p>{{ comments.state.error }}</p>
        <button class="btn btn-secondary" @click="comments.load()">댓글 다시 불러오기</button>
      </div>
      <template v-else>
        <p v-if="!comments.state.content.length" class="meta">아직 작성된 댓글이 없습니다.</p>
        <article v-for="comment in comments.state.content" :key="comment.id" class="comment">
          <div class="meta">
            <AvatarBadge :initial="comment.author.initial" size="26px" font-size="11px" />
            {{ comment.author.nickname }} · {{ comment.createdAtLabel }}
          </div>
          <form v-if="editingCommentId === comment.id && isOwnContent(state, comment)" class="editor" @submit.prevent="saveComment(comment)">
            <label :for="`comment-${comment.id}`">댓글 수정</label>
            <textarea :id="`comment-${comment.id}`" v-model="commentDraft" class="input" required></textarea>
            <div class="actions">
              <button class="btn btn-primary" :disabled="busy || !commentDraft.trim()">저장</button>
              <button type="button" class="btn btn-secondary" :disabled="busy" @click="editingCommentId = null">취소</button>
            </div>
          </form>
          <template v-else>
            <p class="review-body">{{ comment.body }}</p>
            <div v-if="isOwnContent(state, comment)" class="actions">
              <button class="btn btn-ghost" :disabled="busy" @click="editComment(comment)">수정</button>
              <button class="btn btn-ghost" :disabled="busy" @click="removeComment(comment)">삭제</button>
            </div>
          </template>
        </article>
      </template>
      <PaginationControls :page="comments.state.page" :total-pages="comments.state.totalPages" :loading="comments.state.loading || busy" label="댓글" @change="changeCommentPage" />
    </template>
  </main>
</template>

<style scoped>
.review-detail { max-width: 800px; margin: auto; padding: 28px 24px; display: grid; gap: 20px; }
.review-heading { display: flex; gap: 16px; }
.review-heading h3 { margin: 8px 0; }
.editor { display: grid; gap: 10px; }
.actions { display: flex; gap: 8px; }
.review-body { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.9; margin: 0; }
.comment { display: grid; gap: 10px; padding: 16px 0; border-bottom: 1px solid var(--color-divider); }
.error { color: var(--color-danger, #d66); }
</style>
