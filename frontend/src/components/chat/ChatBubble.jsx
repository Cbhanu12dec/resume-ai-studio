import { useAuthStore } from '@/store/authStore'

// Minimal markdown: bold (**text**) and newlines
function renderContent(text) {
  return text
    .split('\n')
    .map((line, i) => {
      const parts = line.split(/\*\*(.*?)\*\*/)
      return (
        <span key={i}>
          {parts.map((part, j) =>
            j % 2 === 1
              ? <strong key={j} className="text-acc3 font-semibold">{part}</strong>
              : part
          )}
          {i < text.split('\n').length - 1 && <br />}
        </span>
      )
    })
}

export default function ChatBubble({ message }) {
  const { user } = useAuthStore()
  const isUser = message.role === 'user'

  return (
    <div className={`flex gap-2 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>
      {/* Avatar */}
      <div className={`
        w-6 h-6 rounded-full shrink-0 flex items-center justify-center text-xs font-bold mt-0.5
        ${isUser ? 'bg-acc/20 text-acc' : 'bg-acc2/20 text-acc2'}
      `}>
        {isUser ? (user?.name?.[0]?.toUpperCase() ?? 'U') : 'AI'}
      </div>

      {/* Bubble */}
      <div className={`
        max-w-[82%] rounded-xl px-3 py-2 text-sm leading-relaxed
        ${isUser
          ? 'bg-acc/15 text-text rounded-tr-sm border border-acc/25'
          : 'bg-bg2 text-text rounded-tl-sm border border-line'
        }
      `}>
        {renderContent(message.content)}
      </div>
    </div>
  )
}
