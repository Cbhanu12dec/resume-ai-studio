import { create } from 'zustand'

let nextId = 1
const makeId = () => `msg_${nextId++}`

export const useChatStore = create((set, get) => ({
  messages: [],
  isTyping: false,
  streamingId: null,

  addUserMessage: (content) => {
    const msg = { id: makeId(), role: 'user', content, ts: Date.now() }
    set((s) => ({ messages: [...s.messages, msg] }))
    return msg
  },

  addAssistantMessage: (content) => {
    const msg = { id: makeId(), role: 'assistant', content, ts: Date.now() }
    set((s) => ({ messages: [...s.messages, msg] }))
    return msg
  },

  /** Start a streaming assistant message — returns the message id */
  startStreaming: () => {
    const id = makeId()
    const msg = { id, role: 'assistant', content: '', ts: Date.now(), streaming: true }
    set((s) => ({ messages: [...s.messages, msg], streamingId: id, isTyping: true }))
    return id
  },

  /** Append a token to the currently streaming message */
  appendToken: (token) => {
    const { streamingId } = get()
    if (!streamingId) return
    set((s) => ({
      messages: s.messages.map((m) =>
        m.id === streamingId ? { ...m, content: m.content + token } : m
      ),
    }))
  },

  /** Mark streaming done, optionally replace content with final text */
  finishStreaming: (finalContent) => {
    const { streamingId } = get()
    if (!streamingId) return
    set((s) => ({
      messages: s.messages.map((m) =>
        m.id === streamingId
          ? { ...m, streaming: false, content: finalContent ?? m.content }
          : m
      ),
      streamingId: null,
      isTyping: false,
    }))
  },

  setTyping: (v) => set({ isTyping: v }),

  clearMessages: () => set({ messages: [], isTyping: false, streamingId: null }),

  getHistory: () =>
    get().messages
      .filter((m) => !m.streaming)
      .map((m) => ({ role: m.role, content: m.content })),
}))
