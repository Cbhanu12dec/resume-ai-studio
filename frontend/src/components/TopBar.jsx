import { useNavigate } from 'react-router-dom'
import { useResumeStore } from '@/store/resumeStore'
import { useAuthStore } from '@/store/authStore'
import { useUpdateLatex } from '@/hooks/useResume'
import { resumeApi } from '@/services/api'
import toast from 'react-hot-toast'

export default function TopBar() {
  const navigate = useNavigate()
  const { activeResume, latexSource, matchResult, isSaving, setCompiling } = useResumeStore()
  const { user, logout } = useAuthStore()
  const updateLatex = useUpdateLatex(activeResume?.id)

  const score = matchResult?.score

  const handleSave = () => {
    if (!activeResume?.id) return
    updateLatex.mutate(latexSource)
  }

  const handleDownload = async () => {
    if (!activeResume?.id) return
    setCompiling(true)
    const toastId = toast.loading('Compiling PDF…')
    try {
      const blob = await resumeApi.downloadPdf(activeResume.id)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${activeResume.originalFileName?.replace(/\.[^/.]+$/, '') ?? 'resume'}.pdf`
      a.click()
      URL.revokeObjectURL(url)
      toast.success('PDF downloaded!', { id: toastId })
    } catch {
      toast.error('PDF compilation failed', { id: toastId })
    } finally {
      setCompiling(false)
    }
  }

  return (
    <header className="h-12 bg-bg1 border-b border-line flex items-center px-4 gap-4 shrink-0">
      {/* Logo */}
      <button
        onClick={() => navigate('/')}
        className="flex items-center gap-2 hover:opacity-80 transition-opacity"
      >
        <div className="w-7 h-7 bg-acc rounded-lg flex items-center justify-center font-black text-white text-xs">
          R
        </div>
        <span className="font-bold text-sm text-text hidden sm:block">
          ResumeAI <span className="text-acc">Studio</span>
        </span>
      </button>

      {/* Filename */}
      {activeResume?.originalFileName && (
        <span className="text-muted text-xs truncate max-w-[140px] hidden md:block">
          {activeResume.originalFileName}
        </span>
      )}

      {/* Spacer */}
      <div className="flex-1" />

      {/* Match score badge */}
      {score != null && (
        <div
          className={`
            px-3 py-1 rounded-full text-xs font-bold border
            ${score >= 80 ? 'bg-success/15 text-success border-success/30'
              : score >= 60 ? 'bg-warn/15 text-warn border-warn/30'
              : 'bg-acc/15 text-acc border-acc/30'}
          `}
        >
          {score}% Match
        </div>
      )}

      {/* Save */}
      <button
        onClick={handleSave}
        disabled={isSaving}
        className="btn-ghost text-xs"
      >
        {isSaving ? 'Saving…' : 'Save'}
      </button>

      {/* Download PDF */}
      <button
        onClick={handleDownload}
        className="btn-primary text-xs"
      >
        ⬇ PDF
      </button>

      {/* User menu */}
      {user && (
        <div className="relative group">
          <button className="w-7 h-7 rounded-full bg-acc2/30 text-acc2 font-bold text-xs flex items-center justify-center hover:bg-acc2/50 transition-colors">
            {user.name?.[0]?.toUpperCase() ?? 'U'}
          </button>
          <div className="absolute right-0 top-full mt-1 w-40 bg-bg2 border border-line rounded-lg py-1 shadow-xl opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none group-hover:pointer-events-auto z-50">
            <div className="px-3 py-1.5 text-xs text-muted border-b border-line">{user.email}</div>
            <button
              onClick={() => { logout(); navigate('/login') }}
              className="w-full text-left px-3 py-1.5 text-sm text-text hover:bg-bg3 transition-colors"
            >
              Sign out
            </button>
          </div>
        </div>
      )}
    </header>
  )
}
