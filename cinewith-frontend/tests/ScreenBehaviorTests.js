import { test, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import { usePagedList } from '../src/composables/usePagedList.js'
import { isOwnContent } from '../src/utils/ownership.js'
import { apiProvider } from '../src/data/apiProvider.js'
import { clearCsrfToken } from '../src/data/apiClient.js'

const originalFetch = globalThis.fetch
afterEach(() => { globalThis.fetch = originalFetch; clearCsrfToken() })
const page = (number, total = 41) => ({ content: [{ id: number }], page: number, size: 20, totalElements: total, totalPages: Math.ceil(total / 20) })
const deferred = () => { let resolve; const promise = new Promise((r) => { resolve = r }); return { promise, resolve } }
const json = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

test('shows owner controls only for the matching registered member', () => {
  const content = { author: { nickname: 'author' } }
  assert.equal(isOwnContent({ status: 'MEMBER', member: { nickname: 'author' } }, content), true)
  for (const auth of [
    { status: 'MEMBER', member: { nickname: 'other' } },
    { status: 'ANONYMOUS', member: { nickname: 'author' } },
    { status: 'SIGNUP_REQUIRED', member: { nickname: 'author' } },
    { status: 'MEMBER', member: null },
  ]) assert.equal(isOwnContent(auth, content), false)
  assert.equal(isOwnContent({ status: 'MEMBER', member: { nickname: 'author' } }, null), false)
})

test('loads requested pages and retains server totals instead of counting visible items', async () => {
  const calls = []
  const list = usePagedList(async (number, size) => { calls.push([number, size]); return page(number) })
  await list.load(0)
  await list.load(1)
  assert.deepEqual(calls, [[0, 20], [1, 20]])
  assert.equal(list.state.page, 1)
  assert.equal(list.state.totalElements, 41)
  assert.equal(list.state.totalPages, 3)
})

test('moves to the last valid page after deletion and handles an empty list', async () => {
  const calls = []
  const list = usePagedList(async (number) => { calls.push(number); return { ...page(number, 20) } })
  await list.load(1)
  assert.deepEqual(calls, [1, 0])
  assert.equal(list.state.page, 0)
  const empty = usePagedList(async (number) => ({ ...page(number, 0), content: [] }))
  await empty.load(2)
  assert.equal(empty.state.page, 0)
  assert.equal(empty.state.totalElements, 0)
  assert.deepEqual(empty.state.content, [])
})

test('keeps the displayed page on failure and clears the error after retry', async () => {
  let fail = false
  const list = usePagedList(async (number) => { if (fail) throw new Error('조회 실패'); return page(number) })
  await list.load(0)
  fail = true
  await list.load(1)
  assert.equal(list.state.page, 0)
  assert.equal(list.state.error, '조회 실패')
  assert.equal(list.state.loading, false)
  fail = false
  await list.load(1)
  assert.equal(list.state.page, 1)
  assert.equal(list.state.error, '')
})

test('ignores stale page responses after a newer request or route reset', async () => {
  const first = deferred()
  const list = usePagedList((number) => number === 0 ? first.promise : Promise.resolve(page(number)))
  const old = list.load(0)
  await list.load(1)
  first.resolve(page(0))
  await old
  assert.equal(list.state.page, 1)
  const pending = deferred()
  const resetList = usePagedList(() => pending.promise)
  const pendingLoad = resetList.load()
  resetList.reset()
  pending.resolve(page(0))
  await pendingLoad
  assert.equal(resetList.state.totalElements, null)
  assert.deepEqual(resetList.state.content, [])
})

test('preserves review and comment pagination metadata across API mapping', async () => {
  const urls = []
  globalThis.fetch = async (url) => {
    urls.push(url)
    return json({ ...page(1), content: [{ reviewId: 9, commentId: 3, authorNickname: 'author', rating: 4.5, content: '본문' }] })
  }
  const reviews = await apiProvider.reviews.listByMovie(550, 1)
  const comments = await apiProvider.comments.list(9, 1)
  assert.deepEqual(urls, ['/api/movies/550/reviews?page=1&size=20', '/api/reviews/9/comments?page=1&size=20'])
  assert.equal(reviews.totalElements, 41)
  assert.equal(comments.totalPages, 3)
  assert.equal(comments.content[0].body, '본문')
  assert.equal(reviews.content[0].likeCount, undefined)
})

test('loads the review feed with its search query and preserves card fields', async () => {
  const urls = []
  globalThis.fetch = async (url) => {
    urls.push(url)
    return json({ ...page(1), content: [{
      reviewId: 9, tmdbId: 550, movieTitle: 'Interstellar', posterPath: '/poster.jpg',
      authorNickname: 'author', rating: 4.5, title: 'Title', contentPreview: 'Preview', createdAt: '2026-09-09T00:00:00Z',
    }] })
  }
  const feed = await apiProvider.reviews.listFeed('space journey', 1)
  assert.deepEqual(urls, ['/api/reviews?page=1&size=20&query=space+journey'])
  assert.equal(feed.content[0].posterPath, '/poster.jpg')
  assert.equal(feed.content[0].excerpt, 'Preview')
  assert.equal(feed.content[0].body[0].text, 'Preview')
})

test('loads a selected discovery section as a paged movie grid', async () => {
  const urls = []
  globalThis.fetch = async (url) => {
    urls.push(url)
    return json({ content: [{ tmdbId: 550, title: 'Movie', posterPath: '/poster.jpg', releaseDate: '2026-09-01' }], page: 1, size: 20, totalElements: 43, totalPages: 3 })
  }
  const result = await apiProvider.movies.discoveryPage('now-playing', 1)
  assert.deepEqual(urls, ['/api/movies/discovery/now-playing?page=1'])
  assert.equal(result.content[0].id, 550)
  assert.equal(result.totalPages, 3)
})

test('resets the feed to page zero when a new search starts and ignores its old response', async () => {
  const old = deferred()
  let query = 'old'
  const list = usePagedList((number) => query === 'old' ? old.promise : Promise.resolve(page(number, 1)))
  const pending = list.load(2)
  query = 'new'
  list.reset()
  await list.load(0)
  old.resolve(page(2, 41))
  await pending
  assert.equal(list.state.page, 0)
  assert.equal(list.state.totalElements, 1)
})

test('does not invent movie statistics or runtime absent from the API', async () => {
  globalThis.fetch = async () => json({ tmdbId: 550, title: 'Movie', posterPath: null, releaseDate: null })
  const movie = await apiProvider.movies.get(550)
  for (const field of ['avgRating', 'reviewCount', 'ratingDistribution', 'runtimeMinutes']) assert.equal(movie[field], undefined)
})

test('sends edited title, content and half-star rating with session credentials and CSRF', async () => {
  const calls = []
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    if (url === '/api/csrf') return json({ headerName: 'X-CSRF-TOKEN', token: 'test-token' })
    return json({ reviewId: 9, authorNickname: 'author', ...JSON.parse(options.body) })
  }
  const input = { title: '수정 제목', content: '수정 본문', rating: 0.5 }
  const result = await apiProvider.reviews.update(9, input)
  assert.equal(result.rating, 0.5)
  assert.equal(calls[1].options.method, 'PATCH')
  assert.equal(calls[1].options.credentials, 'include')
  assert.equal(calls[1].options.headers.get('X-CSRF-TOKEN'), 'test-token')
  assert.deepEqual(JSON.parse(calls[1].options.body), input)
})

test('propagates server ownership and session failures without reporting success', async () => {
  for (const status of [401, 403]) {
    globalThis.fetch = async (url) => url === '/api/csrf'
      ? json({ headerName: 'X-CSRF-TOKEN', token: 'test-token' })
      : json({ code: 'ACCESS_DENIED', message: '권한이 없습니다.' }, status)
    await assert.rejects(apiProvider.reviews.update(9, { rating: 5 }), (error) => error.status === status)
    await assert.rejects(apiProvider.comments.delete(3), (error) => error.status === status)
  }
})
