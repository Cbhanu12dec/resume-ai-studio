import { create } from 'zustand'

let nextId = 1

const makeId = () => `msg_${nextId++}`

export const useChatStore = create((set, get) => ({
  messages: [],
  isTyping: false,

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

  setTyping: (v) => set({ isTyping: v }),

  clearMessages: () => set({ messages: [], isTyping: false }),

  getHistory: () =>
    get().messages.map((m) => ({ role: m.role, content: m.content })),
}))
