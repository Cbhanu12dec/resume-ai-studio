import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuthStore } from '@/store/authStore'
import toast from 'react-hot-toast'

export default function LoginPage() {
  const navigate = useNavigate()
  const { login, register } = useAuthStore()
  const [mode, setMode] = useState('login')   // 'login' | 'register'
  const [loading, setLoading] = useState(false)

  const [form, setForm] = useState({ email: '', password: '', name: '' })
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.email || !form.password) return
    if (mode === 'register' && !form.name) return

    setLoading(true)
    try {
      if (mode === 'login') {
        await login(form.email, form.password)
      } else {
        await register(form.email, form.password, form.name)
      }
      navigate('/')
    } catch (err) {
      toast.error(err.response?.data?.error ?? 'Authentication failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-bg0 grid-bg flex items-center justify-center px-4">
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="w-full max-w-sm"
      >
        {/* Logo */}
        <div className="flex items-center gap-3 mb-8 justify-center">
          <div className="w-10 h-10 bg-acc rounded-xl flex items-center justify-center font-black text-white text-lg glow-acc">
            R
          </div>
          <span className="text-2xl font-black text-text">
            ResumeAI <span className="text-acc">Studio</span>
          </span>
        </div>

        {/* Card */}
        <div className="card p-6">
          {/* Mode toggle */}
          <div className="flex bg-bg3 rounded-lg p-1 mb-6">
            {['login', 'register'].map((m) => (
              <button
                key={m}
                onClick={() => setMode(m)}
                className={`flex-1 py-1.5 rounded-md text-sm font-semibold transition-all ${
                  mode === m ? 'bg-acc text-white' : 'text-muted hover:text-text'
                }`}
              >
                {m === 'login' ? 'Sign in' : 'Sign up'}
              </button>
            ))}
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {mode === 'register' && (
              <div>
                <label className="text-xs text-muted uppercase tracking-wide font-bold block mb-1">Name</label>
                <input
                  type="text"
                  value={form.name}
                  onChange={set('name')}
                  placeholder="Your name"
                  className="input"
                  required
                />
              </div>
            )}
            <div>
              <label className="text-xs text-muted uppercase tracking-wide font-bold block mb-1">Email</label>
              <input
                type="email"
                value={form.email}
                onChange={set('email')}
                placeholder="you@example.com"
                className="input"
                required
              />
            </div>
            <div>
              <label className="text-xs text-muted uppercase tracking-wide font-bold block mb-1">Password</label>
              <input
                type="password"
                value={form.password}
                onChange={set('password')}
                placeholder="••••••••"
                className="input"
                required
                minLength={8}
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="btn-primary w-full justify-center py-2.5 mt-2"
            >
              {loading ? (
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : mode === 'login' ? 'Sign in' : 'Create account'}
            </button>
          </form>
        </div>

        {/* Skip auth (dev mode) */}
        <div className="text-center mt-4">
          <button
            onClick={() => navigate('/')}
            className="text-xs text-muted hover:text-acc3 transition-colors"
          >
            Continue without account →
          </button>
        </div>
      </motion.div>
    </div>
  )
}
