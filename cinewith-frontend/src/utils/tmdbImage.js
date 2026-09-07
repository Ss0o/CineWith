const TMDB_IMAGE_BASE_URL = 'https://image.tmdb.org/t/p/w500'

export function tmdbPosterUrl(posterPath) {
  if (typeof posterPath !== 'string' || !posterPath.trim()) return null
  return `${TMDB_IMAGE_BASE_URL}${posterPath.startsWith('/') ? posterPath : `/${posterPath}`}`
}
