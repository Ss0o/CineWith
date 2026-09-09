import { reactive } from 'vue'

export function useRatingStatistics(fetchStatistics) {
  const state = reactive({ data: null, loading: false, error: '' })
  let version = 0

  async function load(tmdbId) {
    const request = ++version
    Object.assign(state, { data: null, loading: true, error: '' })
    try {
      const data = await fetchStatistics(tmdbId)
      if (request === version) state.data = data
    } catch (error) {
      if (request === version) state.error = error.message
    } finally {
      if (request === version) state.loading = false
    }
  }

  function dispose() { version++ }
  return { state, load, dispose }
}
