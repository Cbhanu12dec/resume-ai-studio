import MessageList from './MessageList'
import QuickActions from './QuickActions'
import ChatInput from './ChatInput'
import { useChatStore } from '@/store/chatStore'

export default function ChatPanel({ resumeId }) {
  const { messages } = useChatStore()

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="px-4 py-3 border-b border-line">
        <p className="text-xs font-bold text-acc uppercase tracking-widest">AI Chat Editor</p>
      </div>

      {/* Messages */}
      <div className="flex-1 min-h-0 overflow-y-auto">
        <MessageList messages={messages} />
      </div>

      {/* Quick actions */}
      <QuickActions resumeId={resumeId} />

      {/* Input */}
      <ChatInput resumeId={resumeId} />
    </div>
  )
}
