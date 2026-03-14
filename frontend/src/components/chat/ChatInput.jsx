import { useState, useRef } from 'react'
import { useWebSocketChat } from '@/hooks/useWebSocketChat'
import { useMatchScore } from '@/hooks/useResume'
import { useChatStore } from '@/store/chatStore'
import { useResumeStore } from '@/store/resumeStore'

export default function ChatInput({ resumeId }) {
  const [value, setValue] = useState('')
  const textareaRef = useRef(null)
  const { sendMessage, isTyping } = useWebSocketChat(resumeId)
  const match = useMatchScore(resumeId)
  const { addUserMessage } = useChatStore()
  const { setActiveTab } = useResumeStore()

  const isPending = isTyping || match.isPending

  const detectAndSendJd = (text) => {
    const jdIndicators = ['job description', 'responsibilities:', 'requirements:', 'qualifications:', 'about the role']
    return jdIndicators.some((kw) => text.toLowerCase().includes(kw))
  }

  const handleSend = () => {
    const trimmed = value.trim()
    if (!trimmed || isPending) return
    setValue('')

    if (detectAndSendJd(trimmed)) {
      addUserMessage(trimmed)
      match.mutate(trimmed, {
        onSuccess: () => setActiveTab('match'),
      })
    } else {
      sendMessage(trimmed)
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const handleInput = (e) => {
    setValue(e.target.value)
    const el = textareaRef.current
    if (el) {
      el.style.height = 'auto'
      el.style.height = Math.min(el.scrollHeight, 120) + 'px'
    }
  }

  return (
    <div className="p-3 border-t border-line">
      <div className="flex items-end gap-2 bg-bg3 border border-line rounded-xl px-3 py-2 focus-within:border-acc3 transition-colors">
        <textarea
          ref={textareaRef}
          value={value}
          onChange={handleInput}
          onKeyDown={handleKeyDown}
          placeholder="Ask or paste JD…"
          rows={1}
          disabled={isPending}
          className="flex-1 bg-transparent text-text text-sm placeholder:text-muted resize-none focus:outline-none leading-relaxed max-h-[120px] disabled:opacity-50"
        />
        <button
          onClick={handleSend}
          disabled={!value.trim() || isPending}
          className="shrink-0 w-7 h-7 bg-acc hover:bg-acc/80 disabled:bg-acc/30 rounded-lg flex items-center justify-center transition-colors"
        >
          {isPending ? (
            <span className="w-3 h-3 border-2 border-white/40 border-t-white rounded-full animate-spin" />
          ) : (
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" className="text-white">
              <path d="M5 12h14M12 5l7 7-7 7" />
            </svg>
          )}
        </button>
      </div>
      <p className="text-xs text-muted mt-1.5 text-center">
        Paste a job description to auto-score your resume
      </p>
    </div>
  )
}
