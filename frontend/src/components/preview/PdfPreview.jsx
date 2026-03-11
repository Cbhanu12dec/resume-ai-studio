import { useState, useEffect, useRef } from 'react'
import { resumeApi } from '@/services/api'
import { useResumeStore } from '@/store/resumeStore'
import toast from 'react-hot-toast'

export default function PdfPreview({ resumeId }) {
  const [pdfUrl, setPdfUrl] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const { setCompiling, latexSource } = useResumeStore()
  const prevLatexRef = useRef(null)

  const compile = async (silent = false) => {
    setLoading(true)
    setError(null)
    setCompiling(true)
    const toastId = silent ? null : toast.loading('Compiling PDF…')
    try {
      const blob = await resumeApi.downloadPdf(resumeId)
      if (pdfUrl) URL.revokeObjectURL(pdfUrl)
      const url = URL.createObjectURL(blob)
      setPdfUrl(url)
      prevLatexRef.current = latexSource
      if (toastId) toast.success('PDF ready', { id: toastId })
    } catch (err) {
      const msg = err?.response?.data?.error || 'Compilation failed'
      setError(msg)
      if (toastId) toast.error(msg, { id: toastId })
    } finally {
      setLoading(false)
      setCompiling(false)
    }
  }

  // Auto-compile on first mount
  useEffect(() => {
    compile(true)
    return () => { if (pdfUrl) URL.revokeObjectURL(pdfUrl) }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [resumeId])

  const isDirty = latexSource && prevLatexRef.current && latexSource !== prevLatexRef.current

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center bg-bg2 h-full">
        <div className="flex flex-col items-center gap-4">
          <div className="w-12 h-12 rounded-full border-4 border-acc/20 border-t-acc animate-spin" />
          <p className="text-muted text-sm">Compiling PDF…</p>
          <p className="text-muted/60 text-xs">First run pulls Docker image — may take ~1 min</p>
        </div>
      </div>
    )
  }

  if (error && !pdfUrl) {
    return (
      <div className="flex items-center justify-center bg-bg2 h-full">
        <div className="text-center space-y-4 max-w-sm px-4">
          <div className="text-5xl">⚠️</div>
          <p className="text-text font-semibold text-sm">Compilation failed</p>
          <p className="text-muted text-xs font-mono bg-bg3 border border-line rounded p-2 text-left break-words">{error}</p>
          <p className="text-muted/70 text-xs">
            Make sure Docker is running or install pdflatex:<br />
            <code className="text-acc">brew install --cask basictex</code>
          </p>
          <button onClick={() => compile()} className="btn-primary text-xs">
            Retry Compile
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col h-full bg-bg2">
      <div className="flex items-center justify-between px-4 py-2 border-b border-line bg-bg1 shrink-0">
        <span className="text-xs text-muted">PDF Preview</span>
        <div className="flex items-center gap-3">
          {isDirty && (
            <span className="text-xs text-yellow-400">● Unsaved changes</span>
          )}
          <button
            onClick={() => compile()}
            disabled={loading}
            className="btn-ghost text-xs"
          >
            {loading ? 'Compiling…' : 'Recompile'}
          </button>
        </div>
      </div>

      {pdfUrl ? (
        <iframe
          src={pdfUrl}
          className="flex-1 w-full border-none bg-white"
          title="Resume PDF Preview"
        />
      ) : (
        <div className="flex items-center justify-center flex-1">
          <button onClick={() => compile()} className="btn-primary">
            Compile PDF
          </button>
        </div>
      )}
    </div>
  )
}
