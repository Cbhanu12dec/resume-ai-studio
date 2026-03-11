import { useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useDropzone } from 'react-dropzone'
import { motion, AnimatePresence } from 'framer-motion'
import { useUploadResume } from '@/hooks/useResume'
import toast from 'react-hot-toast'

const ACCEPTED = {
  'application/pdf':  ['.pdf'],
  'image/png':        ['.png'],
  'image/jpeg':       ['.jpg', '.jpeg'],
  'image/webp':       ['.webp'],
}

const FEATURES = [
  { icon: '⚡', label: 'Parse',  color: 'text-acc',   desc: 'AI extracts all resume data instantly' },
  { icon: '📄', label: 'LaTeX',  color: 'text-acc2',  desc: 'Converts to professional LaTeX format' },
  { icon: '💬', label: 'Chat',   color: 'text-acc3',  desc: 'Edit resume through natural language' },
  { icon: '🎯', label: 'Match',  color: 'text-success',desc: 'Score resume vs job descriptions' },
  { icon: '⬇️', label: 'PDF',   color: 'text-warn',   desc: 'Download ATS-optimized PDF instantly' },
]

const STEPS = [
  { key: 'upload',           label: 'Uploading file',       pct: 15 },
  { key: 'PARSING',          label: 'AI parsing resume…',   pct: 55 },
  { key: 'GENERATING_LATEX', label: 'Generating LaTeX…',    pct: 85 },
  { key: 'READY',            label: 'Done!',                pct: 100 },
]

function stepProgress(status) {
  return STEPS.find((s) => s.key === status)?.pct ?? 5
}

export default function UploadPage() {
  const navigate = useNavigate()
  const [phase, setPhase] = useState(null) // null | 'upload' | status string
  const [dragOver, setDragOver] = useState(false)
  const upload = useUploadResume()

  const handleFile = useCallback(async (file) => {
    setPhase('upload')
    try {
      const resume = await upload.mutateAsync({
        file,
        onProgress: (pct) => { if (pct < 100) { setPhase('upload') } else { setPhase('PARSING') } },
        onStatus: (status) => setPhase(status),
      })
      navigate(`/studio/${resume.id}`)
    } catch {
      setPhase(null)
    }
  }, [upload, navigate])

  const progress = stepProgress(phase)

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop: (accepted) => { if (accepted[0]) handleFile(accepted[0]) },
    onDragEnter: () => setDragOver(true),
    onDragLeave: () => setDragOver(false),
    accept: ACCEPTED,
    maxFiles: 1,
    maxSize: 20 * 1024 * 1024,
    onDropRejected: (files) => {
      const err = files[0]?.errors[0]
      toast.error(err?.code === 'file-too-large' ? 'File too large (max 20MB)' : 'Unsupported file type')
    },
    disabled: upload.isPending,
  })

  const isLoading = upload.isPending || phase !== null

  return (
    <div className="min-h-screen grid-bg flex flex-col items-center justify-center px-4 py-12">
      {/* Logo */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="flex items-center gap-3 mb-10"
      >
        <div className="w-10 h-10 bg-acc rounded-xl flex items-center justify-center font-black text-white text-lg glow-acc">
          R
        </div>
        <span className="text-2xl font-black text-text">
          ResumeAI <span className="text-acc">Studio</span>
        </span>
        <span className="badge-acc ml-2">Beta</span>
      </motion.div>

      {/* Headline */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1, duration: 0.5 }}
        className="text-center mb-8"
      >
        <h1 className="text-4xl font-black text-text mb-2">
          Resume Intelligence Studio
        </h1>
        <p className="text-muted text-sm tracking-wider">
          Upload → AI Parse → LaTeX → Chat Edit → Job Match → Download
        </p>
      </motion.div>

      {/* Drop Zone */}
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ delay: 0.2, duration: 0.4 }}
        className="w-full max-w-xl mb-8"
      >
        <div
          {...getRootProps()}
          className={`
            relative border-2 border-dashed rounded-2xl p-10 text-center cursor-pointer
            transition-all duration-200 select-none
            ${isDragActive || dragOver
              ? 'border-acc bg-acc/5 glow-acc'
              : 'border-line bg-bg1 hover:border-acc/50 hover:bg-bg2'
            }
            ${isLoading ? 'pointer-events-none opacity-60' : ''}
          `}
        >
          <input {...getInputProps()} />

          {/* Pulse ring when idle */}
          {!isLoading && (
            <div className="absolute inset-0 rounded-2xl animate-pulse-ring pointer-events-none" />
          )}

          <AnimatePresence mode="wait">
            {isLoading ? (
              <motion.div
                key="loading"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="flex flex-col items-center gap-4 w-full"
              >
                <div className="w-16 h-16 rounded-full border-4 border-acc/30 border-t-acc animate-spin" />
                <div className="w-full max-w-xs">
                  <div className="flex justify-between text-xs text-muted mb-1">
                    <span>{STEPS.find((s) => s.key === phase)?.label ?? 'Starting…'}</span>
                    <span>{progress}%</span>
                  </div>
                  <div className="w-full bg-bg3 rounded-full h-1.5">
                    <motion.div
                      className="bg-acc h-1.5 rounded-full"
                      animate={{ width: `${progress}%` }}
                      transition={{ ease: 'easeOut', duration: 0.6 }}
                    />
                  </div>
                </div>
                {/* Step indicators */}
                <div className="flex items-center gap-1 text-xs text-muted">
                  {STEPS.filter((s) => s.key !== 'upload').map((s, i) => {
                    const done = progress >= s.pct
                    const active = phase === s.key
                    return (
                      <span key={s.key} className="flex items-center gap-1">
                        {i > 0 && <span className="text-line">›</span>}
                        <span className={active ? 'text-acc font-bold' : done ? 'text-success' : 'text-muted'}>
                          {s.label.replace('…', '')}
                        </span>
                      </span>
                    )
                  })}
                </div>
              </motion.div>
            ) : (
              <motion.div
                key="idle"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="flex flex-col items-center gap-3"
              >
                <div className={`text-5xl transition-transform duration-200 ${isDragActive ? 'scale-125' : ''}`}>
                  {isDragActive ? '🎯' : '📎'}
                </div>
                <p className="text-text font-bold text-lg">
                  {isDragActive ? 'Drop it!' : 'Drop Resume Here'}
                </p>
                <p className="text-muted text-sm">PDF · PNG · JPG · WEBP</p>
                <button
                  type="button"
                  className="btn-primary mt-2"
                  onClick={(e) => { e.stopPropagation() }}
                >
                  Browse Files
                </button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </motion.div>

      {/* Feature pills */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.35, duration: 0.4 }}
        className="flex flex-wrap justify-center gap-3"
      >
        {FEATURES.map((f) => (
          <div
            key={f.label}
            className="group relative flex items-center gap-2 px-4 py-2 bg-bg2 border border-line rounded-xl cursor-default select-none hover:border-acc/30 transition-colors"
          >
            <span className="text-base">{f.icon}</span>
            <span className={`font-bold text-sm ${f.color}`}>{f.label}</span>
            {/* Tooltip */}
            <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-2 py-1 bg-bg3 border border-line rounded text-xs text-text whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-10">
              {f.desc}
            </div>
          </div>
        ))}
      </motion.div>
    </div>
  )
}
