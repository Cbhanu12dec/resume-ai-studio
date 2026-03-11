import axios from 'axios'
import toast from 'react-hot-toast'

const BASE = '/api/v1'

const POLL_INTERVAL_MS = 500
const POLL_TIMEOUT_MS  = 5 * 60 * 1000 // 5 minutes max

const api = axios.create({
  baseURL: BASE,
  timeout: 60_000,
})

// ─── Auth token injection ─────────────────────────────────────────────────
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// ─── Global error handler ─────────────────────────────────────────────────
api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const status = error.response?.status
    const msg = error.response?.data?.error || error.message

    if (status === 401) {
      // Try refresh
      const refresh = localStorage.getItem('refresh_token')
      if (refresh) {
        try {
          const { data } = await axios.post(`${BASE}/auth/refresh`, { refreshToken: refresh })
          localStorage.setItem('access_token', data.data.accessToken)
          error.config.headers.Authorization = `Bearer ${data.data.accessToken}`
          return api.request(error.config)
        } catch {
          localStorage.clear()
          window.location.href = '/login'
        }
      } else {
        window.location.href = '/login'
      }
    } else if (status !== 404) {
      toast.error(msg || 'Something went wrong')
    }

    return Promise.reject(error)
  }
)

// ─── Resume endpoints ─────────────────────────────────────────────────────
export const resumeApi = {
  upload: (file, onProgress) => {
    const form = new FormData()
    form.append('file', file)
    return api.post('/resumes/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => onProgress?.(Math.round((e.loaded * 100) / e.total)),
    }).then((r) => r.data.data)
  },

  getById: (id) => api.get(`/resumes/${id}`).then((r) => r.data.data),

  pollUntilReady: (id, onStatus) =>
    new Promise((resolve, reject) => {
      const deadline = Date.now() + POLL_TIMEOUT_MS
      const timer = setInterval(async () => {
        if (Date.now() > deadline) {
          clearInterval(timer)
          return reject(new Error('Resume processing timed out'))
        }
        try {
          const resume = await resumeApi.getById(id)
          onStatus?.(resume.status)
          if (resume.status === 'READY') {
            clearInterval(timer)
            resolve(resume)
          } else if (resume.status === 'ERROR') {
            clearInterval(timer)
            reject(new Error('Failed to process resume'))
          }
        } catch (e) {
          clearInterval(timer)
          reject(e)
        }
      }, POLL_INTERVAL_MS)
    }),
  list: () => api.get('/resumes').then((r) => r.data.data),
  getLatex: (id) => api.get(`/resumes/${id}/latex`).then((r) => r.data.data),
  updateLatex: (id, latexSource) =>
    api.put(`/resumes/${id}/latex`, { latexSource }).then((r) => r.data.data),
  downloadPdf: (id) =>
    api.get(`/resumes/${id}/pdf`, { responseType: 'blob' }).then((r) => r.data),
  chat: (id, message, history) =>
    api.post(`/resumes/${id}/chat`, { message, history }).then((r) => r.data.data),
  versions: (id) => api.get(`/resumes/${id}/versions`).then((r) => r.data.data),
  restore: (id, versionId) =>
    api.post(`/resumes/${id}/restore/${versionId}`).then((r) => r.data.data),
}

// ─── Match endpoints ──────────────────────────────────────────────────────
export const matchApi = {
  score: (resumeId, jobDescription) =>
    api.post('/match/score', { resumeId, jobDescription }).then((r) => r.data.data),
  history: (resumeId) =>
    api.get('/match/history', { params: { resumeId } }).then((r) => r.data.data),
}

// ─── Auth endpoints ───────────────────────────────────────────────────────
export const authApi = {
  register: (email, password, name) =>
    api.post('/auth/register', { email, password, name }).then((r) => r.data.data),
  login: (email, password) =>
    api.post('/auth/login', { email, password }).then((r) => r.data.data),
  me: () => api.get('/auth/me').then((r) => r.data.data),
}

export default api
