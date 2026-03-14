import { useWebSocketChat } from '@/hooks/useWebSocketChat'
import { useChatStore } from '@/store/chatStore'

const ACTIONS = [
  { label: 'Impactful bullets', prompt: 'Rewrite all bullet points to be more impactful with quantified metrics and strong action verbs.' },
  { label: 'ATS keywords',      prompt: 'Optimize the resume for ATS by adding relevant industry keywords throughout.' },
  { label: 'Summary',           prompt: 'Rewrite the professional summary to be concise, compelling, and tailored for a senior role.' },
  { label: 'Tailored for tech', prompt: 'Tailor the resume for a senior software engineer role at a tech company.' },
  { label: 'Fix formatting',    prompt: 'Fix any formatting issues and ensure consistent punctuation and tense throughout.' },
]

export default function QuickActions({ resumeId }) {
  const { sendMessage, isTyping } = useWebSocketChat(resumeId)

  const handleAction = (prompt) => {
    if (isTyping) return
    sendMessage(prompt)
  }

  return (
    <div className="px-4 py-2 border-t border-line">
      <div className="flex flex-wrap gap-1.5">
        {ACTIONS.map((a) => (
          <button
            key={a.label}
            onClick={() => handleAction(a.prompt)}
            disabled={isTyping}
            className="px-2 py-1 bg-bg3 hover:bg-line text-muted hover:text-text text-xs rounded border border-line hover:border-acc/30 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {a.label}
          </button>
        ))}
      </div>
    </div>
  )
}
