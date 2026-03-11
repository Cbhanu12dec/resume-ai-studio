import { useCallback, useRef } from 'react'
import Editor from '@monaco-editor/react'
import { useResumeStore } from '@/store/resumeStore'
import { useUpdateLatex } from '@/hooks/useResume'
import toast from 'react-hot-toast'

export default function LatexEditor({ resumeId }) {
  const { latexSource, setLatexSource, isSaving } = useResumeStore()
  const updateLatex = useUpdateLatex(resumeId)
  const saveTimer = useRef(null)

  const handleChange = useCallback((value) => {
    setLatexSource(value ?? '')
    // Debounced auto-save after 3s
    clearTimeout(saveTimer.current)
    saveTimer.current = setTimeout(() => {
      if (value?.trim()) updateLatex.mutate(value)
    }, 3000)
  }, [setLatexSource, updateLatex])

  const handleCopy = () => {
    navigator.clipboard.writeText(latexSource)
    toast.success('LaTeX copied!')
  }

  const handleOverleaf = () => {
    const form = document.createElement('form')
    form.method = 'POST'
    form.action = 'https://www.overleaf.com/docs'
    form.target = '_blank'
    const input = document.createElement('input')
    input.type = 'hidden'
    input.name = 'snip'
    input.value = latexSource
    form.appendChild(input)
    document.body.appendChild(form)
    form.submit()
    document.body.removeChild(form)
  }

  return (
    <div className="flex flex-col h-full bg-bg2">
      {/* Toolbar */}
      <div className="flex items-center justify-between px-4 py-2 border-b border-line bg-bg1 shrink-0">
        <div className="flex items-center gap-2">
          <span className="text-xs text-muted font-mono">resume.tex</span>
          {isSaving && <span className="text-xs text-acc3 animate-pulse">saving…</span>}
        </div>
        <div className="flex gap-2">
          <button onClick={handleCopy} className="btn-ghost text-xs">
            Copy LaTeX
          </button>
          <button onClick={handleOverleaf} className="btn-primary text-xs">
            Overleaf ↗
          </button>
        </div>
      </div>

      {/* Editor */}
      <div className="flex-1 min-h-0">
        <Editor
          height="100%"
          language="latex"
          theme="vs-dark"
          value={latexSource}
          onChange={handleChange}
          options={{
            fontSize: 13,
            fontFamily: "'JetBrains Mono', 'Courier New', monospace",
            lineNumbers: 'on',
            minimap: { enabled: false },
            wordWrap: 'on',
            scrollBeyondLastLine: false,
            padding: { top: 16, bottom: 16 },
            renderLineHighlight: 'line',
            smoothScrolling: true,
            cursorBlinking: 'smooth',
            bracketPairColorization: { enabled: true },
          }}
        />
      </div>
    </div>
  )
}
