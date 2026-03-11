import { useEffect, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import ChatBubble from './ChatBubble'
import { useChatStore } from '@/store/chatStore'
import { useResumeStore } from '@/store/resumeStore'

export default function MessageList({ messages }) {
  const bottomRef = useRef(null)
  const { isTyping } = useChatStore()
  const { activeResume } = useResumeStore()

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isTyping])

  if (messages.length === 0 && activeResume) {
    return (
      <div className="p-4 flex flex-col gap-2">
        {/* Welcome assistant message */}
        <ChatBubble
          message={{
            id: 'welcome',
            role: 'assistant',
            content: `Resume parsed! Found **${activeResume?.parsedData?.name ?? 'your resume'}**, ${
              activeResume?.parsedData?.experience?.length ?? 0
            } roles, ${activeResume?.parsedData?.skills?.length ?? 0} skills. How can I improve it?`,
          }}
        />
      </div>
    )
  }

  return (
    <div className="p-4 flex flex-col gap-3">
      <AnimatePresence initial={false}>
        {messages.map((msg) => (
          <motion.div
            key={msg.id}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.2 }}
          >
            <ChatBubble message={msg} />
          </motion.div>
        ))}
      </AnimatePresence>

      {/* Typing indicator */}
      {isTyping && (
        <motion.div
          initial={{ opacity: 0, y: 4 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center gap-2"
        >
          <div className="w-6 h-6 rounded-full bg-acc2/20 flex items-center justify-center text-acc2 text-xs font-bold shrink-0">
            AI
          </div>
          <div className="bg-bg2 border border-line rounded-xl rounded-tl-sm px-3 py-2 flex gap-1 items-center">
            {[0, 1, 2].map((i) => (
              <span
                key={i}
                className="w-1.5 h-1.5 rounded-full bg-muted animate-bounce"
                style={{ animationDelay: `${i * 0.15}s` }}
              />
            ))}
          </div>
        </motion.div>
      )}

      <div ref={bottomRef} />
    </div>
  )
}
