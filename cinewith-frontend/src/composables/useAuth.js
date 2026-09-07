import { reactive, readonly } from 'vue'
import { dataService } from '../data'
import { oauthUrl, ApiError } from '../data/apiClient'

// Mirrors community-service/docs/SECURITY.md's three-state Session model.
export const AUTH_STATE = {
  ANONYMOUS: 'ANONYMOUS',
  SIGNUP_REQUIRED: 'SIGNUP_REQUIRED',
  MEMBER: 'MEMBER',
}

const state = reactive({
  status: AUTH_STATE.ANONYMOUS,
  member: null,
  isLoginModalOpen: false,
  isSignupModalOpen: false,
  error: null,
})

export async function initializeAuth() {
  try {
    state.member = await dataService.auth.me()
    state.status = AUTH_STATE.MEMBER
  } catch (error) {
    if (window.location.pathname === '/signup' && error instanceof ApiError && error.status === 403) {
      state.status = AUTH_STATE.SIGNUP_REQUIRED
      state.isSignupModalOpen = true
      return
    }
    if (error instanceof ApiError && (error.status === 401 || error.status === 403)) return
    state.error = error.message
  }
}

function openLoginModal() {
  state.isLoginModalOpen = true
}

function closeLoginModal() {
  state.isLoginModalOpen = false
}

// Real Spring Security OAuth2 login entry point (see SecurityConfig.java:
// "/oauth2/**" is permitAll and Spring wires /oauth2/authorization/{id}
// automatically once a client registration exists). Safe to link to as-is
// since it's a plain navigation, not an API call this app has to parse.
function googleLoginUrl() {
  return oauthUrl()
}

async function completeSignup(nickname) {
  state.member = await dataService.auth.signup(nickname)
  state.status = AUTH_STATE.MEMBER
  state.isSignupModalOpen = false
  if (window.location.pathname === '/signup') window.history.replaceState({}, '', '/')
}

function closeSignupModal() {
  state.isSignupModalOpen = false
}

async function logout() {
  await dataService.auth.logout()
  state.status = AUTH_STATE.ANONYMOUS
  state.member = null
}

export function useAuth() {
  return {
    state: readonly(state),
    openLoginModal,
    closeLoginModal,
    googleLoginUrl,
    completeSignup,
    closeSignupModal,
    logout,
  }
}
