<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import PosterThumb from './PosterThumb.vue'
import AvatarBadge from './AvatarBadge.vue'
import StarRating from './StarRating.vue'

const props = defineProps({
  review: { type: Object, required: true },
})

const revealed = ref(false)
</script>

<template>
  <div class="rv">
    <PosterThumb width="62px" height="90px" label="포스터" />
    <div style="flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 7px">
      <div style="display: flex; align-items: center; gap: 8px">
        <RouterLink :to="`/movies/${review.movieId}`" style="font: 400 12px/1 var(--font-body); color: color-mix(in srgb, var(--color-text) 55%, transparent)">
          {{ review.movieTitle || `영화 #${review.movieId}` }}
        </RouterLink>
        <span v-if="review.spoiler" class="tag tag-outline">스포일러</span>
      </div>
      <div style="display: flex; align-items: baseline; gap: 10px">
        <RouterLink :to="`/reviews/${review.id}`" style="font: 500 17px/1.3 var(--font-heading); color: var(--color-text)">
          {{ review.title }}
        </RouterLink>
        <StarRating :rating="review.rating" size="12px" />
      </div>

      <div
        v-if="review.spoiler && !revealed"
        style="display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: var(--radius-md); border: 1px dashed var(--color-divider); cursor: pointer"
        @click="revealed = true"
      >
        <i class="ph ph-eye-slash" style="font-size: 15px; color: color-mix(in srgb, var(--color-text) 45%, transparent)"></i>
        <span style="font: 400 12.5px/1 var(--font-body); color: color-mix(in srgb, var(--color-text) 48%, transparent)">스포일러가 포함된 리뷰입니다</span>
        <span style="flex: 1"></span>
        <span class="btn btn-ghost" style="font-size: 12px">눌러서 보기</span>
      </div>
      <p v-else style="font: 400 13px/1.7 var(--font-body); color: color-mix(in srgb, var(--color-text) 68%, transparent); text-wrap: pretty; margin: 0">
        {{ review.excerpt }}
      </p>

      <div class="meta">
        <span style="display: flex; align-items: center; gap: 6px">
          <AvatarBadge :initial="review.author.initial" size="18px" font-size="9px" />{{ review.author.nickname }}
        </span>
        <span>{{ review.createdAtLabel }}</span>
        <span style="display: flex; align-items: center; gap: 5px"><i class="ph ph-heart" style="font-size: 13px"></i>{{ review.likeCount }}</span>
        <span style="display: flex; align-items: center; gap: 5px"><i class="ph ph-chat-circle" style="font-size: 13px"></i>{{ review.commentCount }}</span>
        <span v-if="review.scrapCount" style="display: flex; align-items: center; gap: 5px"><i class="ph ph-bookmark-simple" style="font-size: 13px"></i>{{ review.scrapCount }}</span>
      </div>
    </div>
  </div>
</template>
