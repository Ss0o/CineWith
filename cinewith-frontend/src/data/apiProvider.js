import { apiRequest, clearCsrfToken } from './apiClient.js'

const initial = (nickname = '?') => nickname.charAt(0) || '?'
const dateLabel = (value) => value ? new Date(value).toLocaleDateString('ko-KR') : ''

function movieView(movie) {
  const year = movie.releaseDate ? Number(movie.releaseDate.slice(0, 4)) : null
  return { id: movie.tmdbId, tmdbId: movie.tmdbId, title: movie.title, posterPath: movie.posterPath, releaseDate: movie.releaseDate, year }
}

function reviewView(review) {
  return { id: review.reviewId, reviewId: review.reviewId, movieId: review.tmdbId, tmdbId: review.tmdbId, movieTitle: review.movieTitle, title: review.title, rating: Number(review.rating), author: { nickname: review.authorNickname, initial: initial(review.authorNickname) }, createdAtLabel: dateLabel(review.createdAt), createdAtDate: dateLabel(review.createdAt), updatedAt: review.updatedAt, excerpt: review.content, body: [{ type: 'p', text: review.content }], tags: [], spoiler: false }
}

function commentView(comment) {
  return { id: comment.commentId, commentId: comment.commentId, reviewId: comment.reviewId, author: { nickname: comment.authorNickname, initial: initial(comment.authorNickname) }, createdAtLabel: dateLabel(comment.createdAt), updatedAt: comment.updatedAt, body: comment.content, upvotes: 0, spoiler: false }
}

export const apiProvider = {
  auth: {
    async me() { const member = await apiRequest('/api/members/me'); return { ...member, initial: initial(member.nickname), joinedAt: dateLabel(member.createdAt) } },
    async signup(nickname) { await apiRequest('/api/members/signup', { method: 'POST', body: JSON.stringify({ nickname }) }); return this.me() },
    async logout() { await apiRequest('/api/logout', { method: 'POST' }); clearCsrfToken() },
  },
  movies: {
    async nowPlaying() { return (await apiRequest('/api/movies/now-playing')).map(movieView) },
    async search(query) { return (await apiRequest(`/api/movies/search?query=${encodeURIComponent(query)}`)).map(movieView) },
    async get(id) { return movieView(await apiRequest(`/api/movies/${id}`)) },
    async recommendations(id) { return (await apiRequest(`/api/movies/${id}/recommendations`)).map(movieView) },
  },
  reviews: {
    async listByMovie(movieId, page = 0, size = 20) { const result = await apiRequest(`/api/movies/${movieId}/reviews?page=${page}&size=${size}`); return { ...result, content: result.content.map(reviewView) } },
    async get(id) { return reviewView(await apiRequest(`/api/reviews/${id}`)) },
    async create(input) { return reviewView(await apiRequest('/api/reviews', { method: 'POST', body: JSON.stringify(input) })) },
    async update(id, input) { return reviewView(await apiRequest(`/api/reviews/${id}`, { method: 'PATCH', body: JSON.stringify(input) })) },
    async delete(id) { await apiRequest(`/api/reviews/${id}`, { method: 'DELETE' }) },
  },
  comments: {
    async list(reviewId, page = 0, size = 20) { const result = await apiRequest(`/api/reviews/${reviewId}/comments?page=${page}&size=${size}`); return { ...result, content: result.content.map(commentView) } },
    async create(reviewId, content) { return commentView(await apiRequest(`/api/reviews/${reviewId}/comments`, { method: 'POST', body: JSON.stringify({ content }) })) },
    async update(id, content) { return commentView(await apiRequest(`/api/comments/${id}`, { method: 'PATCH', body: JSON.stringify({ content }) })) },
    async delete(id) { await apiRequest(`/api/comments/${id}`, { method: 'DELETE' }) },
  },
}
