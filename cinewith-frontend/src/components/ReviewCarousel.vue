<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import StarRating from './StarRating.vue'

const props = defineProps({
  reviews: { type: Array, required: true },
})

const track = ref(null)
const hasMoreThanFive = () => props.reviews.length > 5

function move(direction) {
  track.value?.scrollBy({ left: track.value.clientWidth * direction, behavior: 'smooth' })
}
</script>

<template>
  <div class="review-carousel">
    <button v-if="hasMoreThanFive()" class="review-carousel-arrow review-carousel-arrow-left" type="button" aria-label="이전 5개 리뷰" @click="move(-1)">
      <i class="ph ph-caret-left" aria-hidden="true"></i>
    </button>
    <div ref="track" class="review-carousel-track" aria-label="영화 리뷰" tabindex="0">
      <RouterLink v-for="review in reviews" :key="review.id" :to="`/reviews/${review.id}`" class="review-card card">
        <StarRating :rating="review.rating" />
        <strong>{{ review.title }}</strong>
        <p>{{ review.excerpt }}</p>
        <span>{{ review.author.nickname }} · {{ review.createdAtLabel }}</span>
      </RouterLink>
    </div>
    <button v-if="hasMoreThanFive()" class="review-carousel-arrow review-carousel-arrow-right" type="button" aria-label="다음 5개 리뷰" @click="move(1)">
      <i class="ph ph-caret-right" aria-hidden="true"></i>
    </button>
  </div>
</template>

<style scoped>
.review-carousel { position: relative; }
.review-carousel-track {
  display: grid; grid-auto-columns: calc((100% - 48px) / 5); grid-auto-flow: column; gap: 12px;
  overflow-x: auto; overscroll-behavior-x: contain; scroll-behavior: smooth; scroll-snap-type: x mandatory;
  scrollbar-width: none;
}
.review-carousel-track::-webkit-scrollbar { display: none; }
.review-card { min-height: 172px; color: var(--color-text); scroll-snap-align: start; }
.review-card strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; }
.review-card p { display: -webkit-box; margin: 0; overflow: hidden; color: var(--color-neutral-600); font-size: 12px; line-height: 1.65; -webkit-box-orient: vertical; -webkit-line-clamp: 3; }
.review-card span { margin-top: auto; color: var(--color-neutral-600); font-size: 11px; }
.review-carousel-arrow {
  position: absolute; z-index: 1; top: 62px; width: 36px; height: 48px; padding: 0;
  border: 1px solid var(--color-divider); border-radius: 8px; color: var(--color-text);
  background: color-mix(in srgb, var(--color-bg) 92%, transparent); box-shadow: var(--shadow-sm); cursor: pointer;
}
.review-carousel-arrow:hover { color: var(--color-accent); border-color: var(--color-accent); }
.review-carousel-arrow-left { left: -18px; }
.review-carousel-arrow-right { right: -18px; }
@media (max-width: 1100px) { .review-carousel-track { grid-auto-columns: calc((100% - 36px) / 4); } }
@media (max-width: 760px) {
  .review-carousel-track { grid-auto-columns: calc((100% - 12px) / 2); }
  .review-carousel-arrow { display: none; }
}
</style>
