import { test, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import { useRatingStatistics } from '../src/composables/useRatingStatistics.js'
import { apiProvider } from '../src/data/apiProvider.js'

const originalFetch = globalThis.fetch
afterEach(() => { globalThis.fetch = originalFetch })
const distribution = Object.fromEntries(Array.from({ length: 10 }, (_, i) => [((i + 1) / 2).toFixed(1), 0]))
const empty = { averageRating: null, reviewCount: 0, ratingDistribution: distribution }
const deferred = () => { let resolve; const promise = new Promise(r => { resolve = r }); return { resolve, promise } }

test('loads the dedicated statistics endpoint and preserves a null average and ten zero buckets', async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/movies/550/rating-statistics')
    assert.equal(options.method, 'GET')
    return new Response(JSON.stringify(empty), { headers: { 'Content-Type': 'application/json' } })
  }
  assert.deepEqual(await apiProvider.movies.ratingStatistics(550), empty)
})

test('shows loading before rendering the returned average and distribution', async () => {
  const response = deferred()
  const statistics = useRatingStatistics(() => response.promise)
  const pending = statistics.load('550')
  assert.equal(statistics.state.loading, true)
  assert.equal(statistics.state.data, null)
  response.resolve({ averageRating: 4.25, reviewCount: 2, ratingDistribution: { ...distribution, '4.0': 1, '4.5': 1 } })
  await pending
  assert.equal(statistics.state.loading, false)
  assert.equal(statistics.state.data.averageRating, 4.25)
  assert.equal(statistics.state.data.reviewCount, 2)
})

test('represents a failed request separately from no reviews and supports retry', async () => {
  let fail = true
  const statistics = useRatingStatistics(async () => { if (fail) throw Error('통계 조회 실패'); return empty })
  await statistics.load('550')
  assert.equal(statistics.state.data, null)
  assert.equal(statistics.state.error, '통계 조회 실패')
  assert.equal(statistics.state.loading, false)
  fail = false
  await statistics.load('550')
  assert.equal(statistics.state.error, '')
  assert.deepEqual(statistics.state.data, empty)
})

test('ignores the previous movie response after navigation', async () => {
  const old = deferred()
  const statistics = useRatingStatistics(id => id === '550' ? old.promise : Promise.resolve(empty))
  const pending = statistics.load('550')
  await statistics.load('551')
  old.resolve({ averageRating: 5, reviewCount: 1, ratingDistribution: { ...distribution, '5.0': 1 } })
  await pending
  assert.deepEqual(statistics.state.data, empty)
})

test('does not update statistics after the component is disposed', async () => {
  const response = deferred()
  const statistics = useRatingStatistics(() => response.promise)
  const pending = statistics.load('550')
  statistics.dispose()
  response.resolve(empty)
  await pending
  assert.equal(statistics.state.data, null)
})
