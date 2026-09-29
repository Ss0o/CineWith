<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import PosterThumb from './PosterThumb.vue'

defineProps({
  movies: { type: Array, required: true },
  label: { type: String, required: true },
})

const track = ref(null)

function move(direction) {
  track.value?.scrollBy({ left: track.value.clientWidth * direction, behavior: 'smooth' })
}
</script>

<template>
  <div class="movie-carousel">
    <button class="movie-carousel-arrow movie-carousel-arrow-left" type="button" :aria-label="`${label} 이전 5개 영화`" @click="move(-1)">
      <i class="ph ph-caret-left" aria-hidden="true"></i>
    </button>
    <div ref="track" class="movie-carousel-track" :aria-label="label" tabindex="0">
      <RouterLink v-for="movie in movies" :key="movie.tmdbId" :to="`/movies/${movie.tmdbId}`" class="movie-carousel-card card">
        <PosterThumb width="100%" height="214px" :label="movie.title" :poster-path="movie.posterPath" />
        <strong>{{ movie.title }}</strong>
        <span class="meta">{{ movie.releaseDate || '개봉일 미정' }}</span>
      </RouterLink>
    </div>
    <button class="movie-carousel-arrow movie-carousel-arrow-right" type="button" :aria-label="`${label} 다음 5개 영화`" @click="move(1)">
      <i class="ph ph-caret-right" aria-hidden="true"></i>
    </button>
  </div>
</template>

<style scoped>
.movie-carousel { position: relative; }
.movie-carousel-track {
  display: grid; grid-auto-columns: calc((100% - 48px) / 5); grid-auto-flow: column; gap: 12px;
  overflow-x: auto; overscroll-behavior-x: contain; scroll-behavior: smooth; scroll-snap-type: x mandatory;
  scrollbar-width: none;
}
.movie-carousel-track::-webkit-scrollbar { display: none; }
.movie-carousel-card { min-width: 0; color: var(--color-text); scroll-snap-align: start; }
.movie-carousel-card strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
.movie-carousel-arrow {
  position: absolute; z-index: 1; top: 88px; width: 36px; height: 48px; padding: 0;
  border: 1px solid var(--color-divider); border-radius: 8px; color: var(--color-text);
  background: color-mix(in srgb, var(--color-bg) 92%, transparent); box-shadow: var(--shadow-sm); cursor: pointer;
}
.movie-carousel-arrow:hover { color: var(--color-accent); border-color: var(--color-accent); }
.movie-carousel-arrow-left { left: -18px; }
.movie-carousel-arrow-right { right: -18px; }
@media (max-width: 1100px) { .movie-carousel-track { grid-auto-columns: calc((100% - 36px) / 4); } }
@media (max-width: 760px) {
  .movie-carousel-track { grid-auto-columns: calc((100% - 12px) / 2); }
  .movie-carousel-arrow { display: none; }
}
</style>
