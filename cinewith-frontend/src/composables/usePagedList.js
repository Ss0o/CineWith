import { reactive } from 'vue'

export function usePagedList(fetchPage) {
  const state = reactive({ content: [], page: 0, size: 20, totalElements: null, totalPages: 0, loading: false, error: '' })
  let version = 0

  function reset() {
    version++
    Object.assign(state, { content: [], page: 0, totalElements: null, totalPages: 0, loading: false, error: '' })
  }

  async function load(page = state.page) {
    const request = ++version
    state.loading = true
    state.error = ''
    try {
      let result = await fetchPage(page, state.size)
      if (request !== version) return
      // Another user or this screen may have deleted the last item on a page.
      while (result.page > 0 && result.page >= result.totalPages) {
        result = await fetchPage(Math.max(0, result.totalPages - 1), state.size)
        if (request !== version) return
      }
      Object.assign(state, result)
    } catch (error) {
      if (request === version) state.error = error.message
    } finally {
      if (request === version) state.loading = false
    }
  }

  return { state, load, reset }
}
