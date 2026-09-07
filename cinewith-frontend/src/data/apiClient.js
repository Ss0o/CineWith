const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')
let csrf = null

export class ApiError extends Error {
  constructor(status, code, message) {
    super(message || `API 요청에 실패했습니다. (${status})`)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

async function readError(response) {
  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) {
    const body = await response.json()
    return new ApiError(response.status, body.code, body.message)
  }
  return new ApiError(response.status, 'HTTP_ERROR', await response.text())
}

async function csrfToken() {
  if (csrf) return csrf
  const response = await fetch(`${API_BASE_URL}/api/csrf`, { credentials: 'include' })
  if (!response.ok) throw await readError(response)
  csrf = await response.json()
  return csrf
}

export async function apiRequest(path, options = {}) {
  const method = (options.method ?? 'GET').toUpperCase()
  const headers = new Headers(options.headers)

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const token = await csrfToken()
    headers.set(token.headerName, token.token)
  }
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    method,
    headers,
    credentials: 'include',
  })
  if (!response.ok) throw await readError(response)
  if (response.status === 204 || response.headers.get('content-length') === '0') return null
  return response.json()
}

export function oauthUrl() {
  return `${API_BASE_URL}/oauth2/authorization/google`
}

export function clearCsrfToken() {
  csrf = null
}
