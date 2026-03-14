import { useCallback, useRef } from 'react'
import { useChatStore } from '@/store/chatStore'
import { useResumeStore } from '@/store/resumeStore'

/**
 * WebSocket-based streaming chat hook.
 * Opens a new WS connection per message, streams tokens into the chat store,
 * and updates the resume store when the edit is done.
 */
export function useWebSocketChat(resumeId) {
  const { addUserMessage, startStreaming, appendToken, finishStreaming, getHistory, isTyping, setTyping } =
    useChatStore()
  const { updateFromChatEdit } = useResumeStore()
  const wsRef = useRef(null)

  const cancel = useCallback(() => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ type: 'cancel' }))
      wsRef.current.close()
    }
  }, [])

  const sendMessage = useCallback(
    (message) => {
      if (isTyping || !message?.trim()) return

      // Capture history BEFORE adding the user message to avoid sending duplicate
      // user turns (the current message must NOT appear in history — it is sent separately)
      const historySnapshot = getHistory()

      addUserMessage(message.trim())
      startStreaming()

      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const ws = new WebSocket(`${protocol}//${window.location.host}/ws/chat`)
      wsRef.current = ws

      ws.onopen = () => {
        ws.send(
          JSON.stringify({
            resumeId,
            message: message.trim(),
            history: historySnapshot,
          })
        )
      }

      ws.onmessage = (event) => {
        try {
          const frame = JSON.parse(event.data)
          if (frame.type === 'token') {
            appendToken(frame.data)
          } else if (frame.type === 'done') {
            let result = null
            try {
              result = JSON.parse(frame.data)
            } catch {
              result = null
            }
            if (result?.updatedParsedData || result?.updatedLatex) {
              updateFromChatEdit(result)
            }
            finishStreaming()
            ws.close()
          } else if (frame.type === 'error') {
            console.error('Chat WS error:', frame.data)
            finishStreaming(`Error: ${frame.data || 'Something went wrong'}`)
            ws.close()
          }
        } catch (e) {
          console.error('Failed to parse WS frame:', e)
          finishStreaming()
        }
      }

      ws.onerror = (e) => {
        console.error('Chat WS connection error:', e)
        finishStreaming('Connection error — please try again')
      }

      ws.onclose = (e) => {
        if (e.code !== 1000 && e.code !== 1001) {
          // Abnormal close
          const { streamingId } = useChatStore.getState()
          if (streamingId) finishStreaming()
        }
        setTyping(false)
      }
    },
    [
      resumeId,
      isTyping,
      addUserMessage,
      startStreaming,
      appendToken,
      finishStreaming,
      getHistory,
      updateFromChatEdit,
      setTyping,
    ]
  )

  return { sendMessage, cancel, isTyping }
}
