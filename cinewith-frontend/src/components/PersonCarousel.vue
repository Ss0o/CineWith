<script setup>
import { ref } from 'vue'
import { tmdbImageUrl } from '../utils/tmdbImage'

const props = defineProps({
  people: { type: Array, required: true },
})

const track = ref(null)
const hasMoreThanFive = () => props.people.length > 5

function move(direction) {
  track.value?.scrollBy({ left: track.value.clientWidth * direction, behavior: 'smooth' })
}

function profileUrl(profilePath) {
  return tmdbImageUrl(profilePath, 'w185')
}
</script>

<template>
  <div class="person-carousel">
    <button v-if="hasMoreThanFive()" class="person-carousel-arrow person-carousel-arrow-left" type="button" aria-label="이전 5명" @click="move(-1)">
      <i class="ph ph-caret-left" aria-hidden="true"></i>
    </button>
    <div ref="track" class="person-carousel-track" aria-label="출연진" tabindex="0">
      <article v-for="person in people" :key="`${person.personId}-${person.character}`" class="person-card card">
        <img v-if="profileUrl(person.profilePath)" :src="profileUrl(person.profilePath)" :alt="`${person.name} 프로필`" loading="lazy" @error="$event.target.remove()" />
        <span v-else class="person-placeholder" aria-hidden="true">{{ person.name?.slice(0, 1) || '?' }}</span>
        <strong>{{ person.name }}</strong>
        <span>{{ person.character || '배역 정보 없음' }}</span>
      </article>
    </div>
    <button v-if="hasMoreThanFive()" class="person-carousel-arrow person-carousel-arrow-right" type="button" aria-label="다음 5명" @click="move(1)">
      <i class="ph ph-caret-right" aria-hidden="true"></i>
    </button>
  </div>
</template>

<style scoped>
.person-carousel { position: relative; }
.person-carousel-track {
  display: grid; grid-auto-columns: calc((100% - 48px) / 5); grid-auto-flow: column; gap: 12px;
  overflow-x: auto; overscroll-behavior-x: contain; scroll-behavior: smooth; scroll-snap-type: x mandatory;
  scrollbar-width: none;
}
.person-carousel-track::-webkit-scrollbar { display: none; }
.person-card { min-width: 0; box-shadow: var(--shadow-sm); scroll-snap-align: start; }
.person-card img, .person-placeholder { width: 100%; aspect-ratio: .82; margin-bottom: 7px; border-radius: var(--radius-md); background: var(--color-neutral-200); object-fit: cover; }
.person-placeholder { display: grid; place-items: center; color: var(--color-neutral-600); font-size: 32px; }
.person-card strong, .person-card span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.person-card strong { color: var(--color-text); font-size: 14px; }
.person-card span:last-child { color: var(--color-neutral-600); font-size: 12px; }
.person-carousel-arrow {
  position: absolute; z-index: 1; top: 88px; width: 36px; height: 48px; padding: 0;
  border: 1px solid var(--color-divider); border-radius: 8px; color: var(--color-text);
  background: color-mix(in srgb, var(--color-bg) 92%, transparent); box-shadow: var(--shadow-sm); cursor: pointer;
}
.person-carousel-arrow:hover { color: var(--color-accent); border-color: var(--color-accent); }
.person-carousel-arrow-left { left: -18px; }
.person-carousel-arrow-right { right: -18px; }
@media (max-width: 1100px) { .person-carousel-track { grid-auto-columns: calc((100% - 36px) / 4); } }
@media (max-width: 760px) {
  .person-carousel-track { grid-auto-columns: calc((100% - 12px) / 2); }
  .person-carousel-arrow { display: none; }
}
</style>
