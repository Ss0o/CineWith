const TMDB_IMAGE_BASE_URL = 'https://image.tmdb.org/t/p'

export function tmdbImageUrl(path, size = 'w500') {
  if (typeof path !== 'string' || !path.trim()) return null
  return `${TMDB_IMAGE_BASE_URL}/${size}${path.startsWith('/') ? path : `/${path}`}`
}

export function tmdbPosterUrl(posterPath) {
  return tmdbImageUrl(posterPath)
}

export function tmdbBackdropUrl(backdropPath) {
  return tmdbImageUrl(backdropPath, 'w1280')
}
